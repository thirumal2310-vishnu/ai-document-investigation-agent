
package com.responsive.assessment.repository;

import com.responsive.assessment.entity.InvestigationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestigationRunRepository
        extends JpaRepository<InvestigationRun, Long> {

    List<InvestigationRun> findAllByOwnerIdOrderByCreatedAtDesc(
            String ownerId);

    Optional<InvestigationRun> findByIdAndOwnerId(
            Long id, String ownerId);
}
