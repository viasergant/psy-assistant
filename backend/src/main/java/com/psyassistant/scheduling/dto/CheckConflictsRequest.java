package com.psyassistant.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Request DTO for checking appointment conflicts before creating.
 *
 * <p>Used as a pre-flight check to show conflicts to the user before submission.
 */
public record CheckConflictsRequest(
        @NotNull(message = "Therapist profile ID is required")
        UUID therapistProfileId,

        @NotNull(message = "Start time is required")
        ZonedDateTime startTime,

        @NotNull(message = "Duration is required")
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 480, message = "Duration cannot exceed 8 hours")
        Integer durationMinutes
) {
    /**
     * Compact constructor for validation.
     */
    public CheckConflictsRequest {
        if (durationMinutes != null && durationMinutes % 15 != 0) {
            throw new IllegalArgumentException("Duration must be a multiple of 15 minutes");
        }
    }
}
