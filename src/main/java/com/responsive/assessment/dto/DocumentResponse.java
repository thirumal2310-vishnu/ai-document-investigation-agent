
package com.responsive.assessment.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String fileName,
        String ownerId,
        LocalDateTime uploadedAt) {
}
