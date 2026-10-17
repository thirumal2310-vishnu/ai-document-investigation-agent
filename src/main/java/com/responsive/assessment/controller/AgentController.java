package com.responsive.assessment.controller;

import com.responsive.assessment.dto.InvestigationRequest;
import com.responsive.assessment.dto.InvestigationResponse;
import com.responsive.assessment.service.AgentService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/investigate")
    public InvestigationResponse investigate(
            @RequestHeader("X-User-Id") String ownerId,
            @Valid @RequestBody InvestigationRequest request) {

        return agentService.investigate(ownerId, request);
    }
}