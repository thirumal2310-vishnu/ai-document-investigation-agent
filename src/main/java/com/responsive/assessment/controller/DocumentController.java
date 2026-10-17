package com.responsive.assessment.controller;

import com.responsive.assessment.dto.DocumentResponse;
import com.responsive.assessment.entity.DocumentRecord;
import com.responsive.assessment.service.DocumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1 MB

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestParam("file") MultipartFile file) {

        validateOwnerId(ownerId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "File size must not exceed 1 MB");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null
                || fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")
                || !fileName.toLowerCase().endsWith(".txt")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only valid .txt files are supported");
        }

      
try {
    byte[] bytes = file.getBytes();

    String content = StandardCharsets.UTF_8
            .newDecoder()
            .decode(ByteBuffer.wrap(bytes))
            .toString();

    // Remove UTF-8 BOM if present at the beginning of the file
    if (content.startsWith("\uFEFF")) {
        content = content.substring(1);
    }

    if (content.isBlank()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File content must not be blank");
    }


            if (content.indexOf('\0') >= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "File contains invalid content");
            }

            DocumentRecord saved = documentService.upload(
                    fileName, ownerId, content);

            return toResponse(saved);

        } catch (CharacterCodingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File must contain valid UTF-8 text");
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read uploaded file");
        }
    }

    @GetMapping
    public List<DocumentResponse> getDocuments(
            @RequestHeader("X-User-Id") String ownerId) {

        validateOwnerId(ownerId);

        return documentService.getDocuments(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse getDocument(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String ownerId) {

        validateOwnerId(ownerId);

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Document ID must be positive");
        }

        DocumentRecord document = documentService
                .getDocument(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        return toResponse(document);
    }

    private void validateOwnerId(String ownerId) {
        if (ownerId == null
                || ownerId.isBlank()
                || ownerId.length() > 100
                || ownerId.chars().anyMatch(Character::isISOControl)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid X-User-Id");
        }
    }

    private DocumentResponse toResponse(DocumentRecord document) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getOwnerId(),
                document.getUploadedAt()
        );
    }
}