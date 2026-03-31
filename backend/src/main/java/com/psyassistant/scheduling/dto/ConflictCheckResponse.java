package com.psyassistant.scheduling.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for conflict check results.
 *
 * <p>Includes list of conflicting appointments with minimal details for UI display.
 */
public record ConflictCheckResponse(
        boolean hasConflicts,
        List<ConflictingAppointment> conflicts
) {
    /**
     * Minimal appointment details for conflict display.
     */
    public record ConflictingAppointment(
            UUID id,
            UUID clientId,
            String clientName,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            Integer durationMinutes
    ) {
    }
}
