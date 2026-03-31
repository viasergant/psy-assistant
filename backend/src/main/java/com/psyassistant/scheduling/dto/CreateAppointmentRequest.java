package com.psyassistant.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a new appointment.
 *
 * <p>All fields are validated with Bean Validation annotations.
 */
public record CreateAppointmentRequest(
        @NotNull(message = "Therapist profile ID is required")
        UUID therapistProfileId,

        @NotNull(message = "Client ID is required")
        UUID clientId,

        @NotNull(message = "Session type ID is required")
        UUID sessionTypeId,

        @NotNull(message = "Start time is required")
        ZonedDateTime startTime,

        @NotNull(message = "Duration is required")
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 480, message = "Duration cannot exceed 8 hours")
        Integer durationMinutes,

        @NotNull(message = "Timezone is required")
        String timezone,

        String notes,

        Boolean allowConflictOverride
) {
    /**
     * Compact constructor for validation.
     */
    public CreateAppointmentRequest {
        // Validate duration increment
        if (durationMinutes != null && durationMinutes % 15 != 0) {
            throw new IllegalArgumentException("Duration must be a multiple of 15 minutes");
        }

        // Default conflict override to false if not provided
        if (allowConflictOverride == null) {
            allowConflictOverride = false;
        }
    }
}
