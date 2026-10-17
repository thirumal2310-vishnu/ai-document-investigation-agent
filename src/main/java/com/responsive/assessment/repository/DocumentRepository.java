
package com.responsive.assessment.repository;

import com.responsive.assessment.entity.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<DocumentRecord, Long> {

    List<DocumentRecord> findAllByOwnerId(String ownerId);

    Optional<DocumentRecord> findByIdAndOwnerId(
            Long id, String ownerId);
}
