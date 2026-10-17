package com.responsive.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InvestigationRequest(
        @NotBlank
        @Size(min = 5, max = 500)
        String question,

        @NotEmpty
        @Size(max = 20)
        List<Long> documentIds
) {
}