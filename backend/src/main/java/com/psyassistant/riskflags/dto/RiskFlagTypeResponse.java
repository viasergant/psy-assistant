package com.psyassistant.riskflags.dto;

import java.util.UUID;

/**
 * Response DTO for a risk flag type.
 *
 * @param id           flag type UUID
 * @param name         unique display label
 * @param displayOrder relative sort position in pick-lists
 * @param active       whether this type is available for new flags
 */
public record RiskFlagTypeResponse(
        UUID id,
        String name,
        int displayOrder,
        boolean active
) {
}
