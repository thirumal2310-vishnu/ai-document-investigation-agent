package com.responsive.assessment.dto;

import java.util.List;

public record InvestigationResponse(
        String conclusion,
        List<EvidenceItem> evidence,
        List<String> conflicts,
        List<String> missingInformation,
        String nextAction
) {
}