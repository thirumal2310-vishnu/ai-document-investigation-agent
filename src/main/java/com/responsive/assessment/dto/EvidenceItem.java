package com.responsive.assessment.dto;

public record EvidenceItem(
        Long documentId,
        String fileName,
        String excerpt
) {
}