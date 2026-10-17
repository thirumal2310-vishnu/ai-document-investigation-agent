
package com.responsive.assessment.controller;

import com.responsive.assessment.entity.Investigation;
import com.responsive.assessment.service.InvestigationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final InvestigationService investigationService;

    public InvestigationController(
            InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Investigation createInvestigation(
            @RequestBody CreateInvestigationRequest request) {
        return investigationService.createInvestigation(
                request.documentName());
    }

    @GetMapping
    public List<Investigation> getAllInvestigations() {
        return investigationService.getAllInvestigations();
    }

    @GetMapping("/{id}")
    public Investigation getInvestigationById(@PathVariable Long id) {
        return investigationService.getInvestigationById(id)
                .orElseThrow(() ->
                        new InvestigationNotFoundException(id));
    }

    public record CreateInvestigationRequest(String documentName) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class InvestigationNotFoundException
            extends RuntimeException {
        public InvestigationNotFoundException(Long id) {
            super("Investigation not found with id: " + id);
        }
    }
}
