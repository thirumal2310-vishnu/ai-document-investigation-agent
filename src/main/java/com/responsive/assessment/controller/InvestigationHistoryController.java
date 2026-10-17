
package com.responsive.assessment.controller;

import com.responsive.assessment.entity.InvestigationRun;
import com.responsive.assessment.repository.InvestigationRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/agent/history")
public class InvestigationHistoryController {

    private final InvestigationRunRepository investigationRunRepository;

    public InvestigationHistoryController(
            InvestigationRunRepository investigationRunRepository) {
        this.investigationRunRepository = investigationRunRepository;
    }

    @GetMapping
    public List<InvestigationRun> getHistory(
            @RequestHeader("X-User-Id") String ownerId) {

        validateOwnerId(ownerId);

        return investigationRunRepository
                .findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @GetMapping("/{id}")
    public InvestigationRun getHistoryById(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable Long id) {

        validateOwnerId(ownerId);

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid investigation ID");
        }

        return investigationRunRepository
                .findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Investigation history not found"));
    }

    private void validateOwnerId(String ownerId) {
        if (ownerId == null
                || ownerId.isBlank()
                || ownerId.length() > 100
                || ownerId.chars().anyMatch(Character::isISOControl)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid X-User-Id");
        }
    }
}
