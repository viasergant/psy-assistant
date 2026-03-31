package com.psyassistant.scheduling.dto;

import java.util.UUID;

/**
 * Response DTO for session type information.
 *
 * <p>Used for populating session type dropdown menus in the scheduling UI.
 */
public record SessionTypeResponse(
        UUID id,
        String code,
        String name,
        String description
) {
}
