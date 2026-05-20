package com.psyassistant.riskflags.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for resolving an active risk flag.
 *
 * @param resolutionNote explanation of how the risk was resolved; must not be blank
 */
public record ResolveRiskFlagRequest(
        @NotBlank String resolutionNote
) {
}
