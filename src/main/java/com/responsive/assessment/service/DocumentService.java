
package com.responsive.assessment.service;

import com.responsive.assessment.entity.DocumentRecord;
import com.responsive.assessment.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional
    public DocumentRecord upload(
            String fileName, String ownerId, String content) {

        DocumentRecord document =
                new DocumentRecord(fileName, ownerId, content);

        return documentRepository.save(document);
    }

    public List<DocumentRecord> getDocuments(String ownerId) {
        return documentRepository.findAllByOwnerId(ownerId);
    }

    public Optional<DocumentRecord> getDocument(
            Long id, String ownerId) {
        return documentRepository.findByIdAndOwnerId(id, ownerId);
    }
}
