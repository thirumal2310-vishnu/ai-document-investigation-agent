
package com.responsive.assessment.service;

import com.responsive.assessment.entity.Investigation;
import com.responsive.assessment.repository.InvestigationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvestigationService {

    private final InvestigationRepository investigationRepository;

    public InvestigationService(
            InvestigationRepository investigationRepository) {
        this.investigationRepository = investigationRepository;
    }

    public Investigation createInvestigation(String documentName) {
        Investigation investigation =
                new Investigation(documentName, "PENDING");

        return investigationRepository.save(investigation);
    }

    public List<Investigation> getAllInvestigations() {
        return investigationRepository.findAll();
    }

    public Optional<Investigation> getInvestigationById(Long id) {
        return investigationRepository.findById(id);
    }
}
