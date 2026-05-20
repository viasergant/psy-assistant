package com.psyassistant.riskflags.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new risk flag type.
 *
 * @param name         unique display label; must not be blank, max 100 chars
 * @param displayOrder relative sort position in pick-lists; must be zero or positive
 */
public record CreateRiskFlagTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @Min(0) int displayOrder
) {
}
