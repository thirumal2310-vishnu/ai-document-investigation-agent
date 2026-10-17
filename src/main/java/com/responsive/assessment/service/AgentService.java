
package com.responsive.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.responsive.assessment.dto.EvidenceItem;
import com.responsive.assessment.dto.InvestigationRequest;
import com.responsive.assessment.dto.InvestigationResponse;
import com.responsive.assessment.entity.DocumentRecord;
import com.responsive.assessment.entity.InvestigationRun;
import com.responsive.assessment.repository.DocumentRepository;
import com.responsive.assessment.repository.InvestigationRunRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_EVIDENCE_ITEMS = 10;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "is", "are", "was", "were", "has", "have",
            "had", "been", "being", "a", "an", "and", "or",
            "of", "to", "in", "on", "for", "from", "with",
            "by", "it", "this", "that", "what", "when",
            "where", "who", "how", "does", "do", "did",
            "me", "my", "we", "our", "can", "could",
            "would", "should", "please", "document",
            "tell", "show", "find", "explain", "about"
    );

    private static final Pattern KEY_VALUE_PATTERN =
            Pattern.compile("^\\s*([^:]{2,60}):\\s*(.+?)\\s*$");

    private static final Pattern INVOICE_NUMBER_PATTERN =
            Pattern.compile("\\b(?:INV[- ]?\\d+)\\b",
                    Pattern.CASE_INSENSITIVE);

    private final DocumentRepository documentRepository;
    private final InvestigationRunRepository investigationRunRepository;
    private final ObjectMapper objectMapper;

    public AgentService(
            DocumentRepository documentRepository,
            InvestigationRunRepository investigationRunRepository,
            ObjectMapper objectMapper) {

        this.documentRepository = documentRepository;
        this.investigationRunRepository = investigationRunRepository;
        this.objectMapper = objectMapper;
    }

    public InvestigationResponse investigate(
            String ownerId,
            InvestigationRequest request) {

        validateRequest(ownerId, request);

        List<DocumentRecord> documents = new ArrayList<>();

        // Deduplicate document IDs and verify document ownership.
        for (Long documentId : new LinkedHashSet<>(request.documentIds())) {
            Optional<DocumentRecord> document =
                    documentRepository.findByIdAndOwnerId(
                            documentId, ownerId);

            documents.add(document.orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "One or more documents were not found")));
        }

        List<String> questionTerms = extractTerms(request.question());

        String normalizedQuestion =
                request.question().toLowerCase(Locale.ROOT);

        List<EvidenceItem> evidence = findEvidence(
                documents, questionTerms, normalizedQuestion);

        List<String> conflicts = detectConflicts(documents);

        List<String> missingInformation = new ArrayList<>();

        if (evidence.isEmpty()) {
            missingInformation.add(
                    "The selected documents do not contain clearly relevant "
                            + "evidence for this question.");
        }

        if (documents.size() == 1) {
            missingInformation.add(
                    "Only one document was provided. Independent "
                            + "confirmation is unavailable.");
        }

        if (!conflicts.isEmpty()) {
            missingInformation.add(
                    "Conflicting values require verification against "
                            + "an authoritative source.");
        } else if (!evidence.isEmpty()) {
            missingInformation.add(
                    "The evidence has not been independently verified.");
        }

        String conclusion = buildConclusion(
                evidence, conflicts, normalizedQuestion);

        String nextAction = buildNextAction(evidence, conflicts);

        InvestigationResponse response = new InvestigationResponse(
                conclusion,
                evidence,
                conflicts,
                missingInformation,
                nextAction
        );

        // Save the completed investigation before returning the response.
        saveInvestigation(ownerId, request.question(), response);

        return response;
    }

    private void validateRequest(
            String ownerId,
            InvestigationRequest request) {

        if (ownerId == null
                || ownerId.isBlank()
                || ownerId.length() > 100
                || ownerId.chars().anyMatch(Character::isISOControl)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid X-User-Id");
        }

        if (request == null
                || request.question() == null
                || request.question().isBlank()
                || request.question().length() > MAX_QUESTION_LENGTH) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid investigation question");
        }

        if (request.documentIds() == null
                || request.documentIds().isEmpty()
                || request.documentIds().size() > 20
                || request.documentIds().stream()
                        .anyMatch(id -> id == null || id <= 0)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid document IDs");
        }

        String question =
                request.question().toLowerCase(Locale.ROOT);

        if (question.matches(
                ".*\\b(passwords?|secret keys?|api keys?|access tokens?|private keys?)\\b.*")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Investigations seeking credentials or secrets are not supported");
        }
    }

    private List<EvidenceItem> findEvidence(
            List<DocumentRecord> documents,
            List<String> questionTerms,
            String normalizedQuestion) {

        List<EvidenceItem> evidence = new ArrayList<>();

        boolean asksAboutPayment =
                normalizedQuestion.contains("paid")
                        || normalizedQuestion.contains("payment")
                        || normalizedQuestion.contains("invoice");

        Set<String> requestedInvoiceNumbers =
                extractInvoiceNumbers(normalizedQuestion);

        for (DocumentRecord document : documents) {

            if (document.getContent() == null) {
                continue;
            }

            for (String line : document.getContent().split("\\R")) {

                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String normalizedLine =
                        trimmedLine.toLowerCase(Locale.ROOT);

                long score = questionTerms.stream()
                        .filter(normalizedLine::contains)
                        .distinct()
                        .count();

                Set<String> lineInvoiceNumbers =
                        extractInvoiceNumbers(normalizedLine);

                boolean invoiceMatches =
                        !requestedInvoiceNumbers.isEmpty()
                                && lineInvoiceNumbers.stream()
                                        .anyMatch(
                                                requestedInvoiceNumbers::contains);

                if (!requestedInvoiceNumbers.isEmpty()
                        && !invoiceMatches) {

                    if (lineInvoiceNumbers.isEmpty()) {
                        score = Math.max(0, score - 1);
                    } else {
                        continue;
                    }
                }

                if (invoiceMatches) {
                    score += 8;
                }

                if (asksAboutPayment
                        && normalizedLine.contains("payment status")) {
                    score += 10;
                }

                if (asksAboutPayment
                        && (normalizedLine.contains("paid")
                        || normalizedLine.contains("pending")
                        || normalizedLine.contains("unpaid"))) {
                    score += 4;
                }

                if (score > 0) {
                    evidence.add(new EvidenceItem(
                            document.getId(),
                            document.getFileName(),
                            trimmedLine
                    ));
                }
            }
        }

        evidence.sort((first, second) -> {
            long firstScore = score(
                    first.excerpt(), questionTerms, normalizedQuestion);

            long secondScore = score(
                    second.excerpt(), questionTerms, normalizedQuestion);

            return Long.compare(secondScore, firstScore);
        });

        return evidence.stream()
                .limit(MAX_EVIDENCE_ITEMS)
                .toList();
    }

    private long score(
            String text,
            List<String> questionTerms,
            String normalizedQuestion) {

        String normalizedText = text.toLowerCase(Locale.ROOT);

        long score = questionTerms.stream()
                .filter(normalizedText::contains)
                .distinct()
                .count();

        boolean asksAboutPayment =
                normalizedQuestion.contains("paid")
                        || normalizedQuestion.contains("payment")
                        || normalizedQuestion.contains("invoice");

        if (asksAboutPayment
                && normalizedText.contains("payment status")) {
            score += 10;
        }

        if (asksAboutPayment
                && (normalizedText.contains("paid")
                || normalizedText.contains("pending")
                || normalizedText.contains("unpaid"))) {
            score += 4;
        }

        Set<String> requestedInvoiceNumbers =
                extractInvoiceNumbers(normalizedQuestion);

        if (!requestedInvoiceNumbers.isEmpty()
                && extractInvoiceNumbers(normalizedText)
                        .stream()
                        .anyMatch(requestedInvoiceNumbers::contains)) {
            score += 8;
        }

        return score;
    }

    private Set<String> extractInvoiceNumbers(String text) {

        Set<String> invoiceNumbers = new LinkedHashSet<>();

        Matcher matcher = INVOICE_NUMBER_PATTERN.matcher(text);

        while (matcher.find()) {
            invoiceNumbers.add(
                    matcher.group()
                            .replace(" ", "-")
                            .toUpperCase(Locale.ROOT));
        }

        return invoiceNumbers;
    }

    private List<String> extractTerms(String question) {

        return Arrays.stream(
                        question.toLowerCase(Locale.ROOT)
                                .replaceAll("[^a-z0-9\\s-]", " ")
                                .split("\\s+"))
                .filter(term -> term.length() > 1)
                .filter(term -> !STOP_WORDS.contains(term))
                .distinct()
                .toList();
    }

    private List<String> detectConflicts(
            List<DocumentRecord> documents) {

        Map<String, Map<String, Set<String>>> facts =
                new LinkedHashMap<>();

        for (DocumentRecord document : documents) {

            if (document.getContent() == null) {
                continue;
            }

            for (String line : document.getContent().split("\\R")) {

                Matcher matcher = KEY_VALUE_PATTERN.matcher(line);

                if (!matcher.matches()) {
                    continue;
                }

                String key = normalizeFieldName(matcher.group(1));
                String value = matcher.group(2).trim();

                if (key.isBlank() || value.isBlank()) {
                    continue;
                }

                facts.computeIfAbsent(
                                key, ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(
                                value.toLowerCase(Locale.ROOT),
                                ignored -> new LinkedHashSet<>())
                        .add(document.getFileName() + ": " + value);
            }
        }

        List<String> conflicts = new ArrayList<>();

        facts.forEach((key, values) -> {

            if (values.size() > 1) {

                String details = values.values().stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.joining("; "));

                conflicts.add(
                        "Potential conflict for '" + key + "': " + details);
            }
        });

        return conflicts;
    }

    private String normalizeFieldName(String fieldName) {

        return fieldName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String buildConclusion(
            List<EvidenceItem> evidence,
            List<String> conflicts,
            String normalizedQuestion) {

        if (!conflicts.isEmpty()) {
            return "The selected documents contain potentially conflicting "
                    + "facts. A definitive conclusion cannot be made "
                    + "until the discrepancies are verified.";
        }

        if (evidence.isEmpty()) {
            return "Insufficient evidence: the selected documents do not "
                    + "contain clearly relevant information to answer "
                    + "the question.";
        }

        boolean asksAboutPayment =
                normalizedQuestion.contains("paid")
                        || normalizedQuestion.contains("payment")
                        || normalizedQuestion.contains("invoice");

        Optional<String> paymentStatus = evidence.stream()
                .map(EvidenceItem::excerpt)
                .filter(line -> line.toLowerCase(Locale.ROOT)
                        .contains("payment status"))
                .findFirst();

        if (asksAboutPayment && paymentStatus.isPresent()) {
            return "The document reports the following payment information: "
                    + paymentStatus.get()
                    + ". This reflects the document's contents and does not "
                    + "independently confirm the actual payment.";
        }

        String excerpts = evidence.stream()
                .limit(3)
                .map(EvidenceItem::excerpt)
                .collect(Collectors.joining("; "));

        return "The available document evidence states: "
                + excerpts
                + ". This conclusion is based only on matching text "
                + "and is not independent verification.";
    }

    private String buildNextAction(
            List<EvidenceItem> evidence,
            List<String> conflicts) {

        if (!conflicts.isEmpty()) {
            return "Compare the conflicting facts with the original "
                    + "documents or an authoritative system and resolve "
                    + "the discrepancy.";
        }

        if (evidence.isEmpty()) {
            return "Provide a more relevant document or clarify the "
                    + "question so the investigation can be narrowed down.";
        }

        return "Review the cited document excerpts and verify important "
                + "claims with the relevant authoritative source.";
    }

    private void saveInvestigation(
            String ownerId,
            String question,
            InvestigationResponse response) {

        try {
            InvestigationRun run = new InvestigationRun();

            run.setOwnerId(ownerId);
            run.setQuestion(question);
            run.setConclusion(response.conclusion());

            run.setEvidenceJson(
                    objectMapper.writeValueAsString(response.evidence()));

            run.setConflictsJson(
                    objectMapper.writeValueAsString(response.conflicts()));

            run.setMissingInformationJson(
                    objectMapper.writeValueAsString(
                            response.missingInformation()));

            run.setNextAction(response.nextAction());

            investigationRunRepository.save(run);

        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to serialize investigation results");
        }
    }
}
