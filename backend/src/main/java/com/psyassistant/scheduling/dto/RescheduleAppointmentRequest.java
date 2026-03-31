package com.psyassistant.scheduling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;

/**
 * Request DTO for rescheduling an existing appointment.
 *
 * <p>Requires the new start time and a reason for the reschedule.
 * Conflict detection is applied to the new time slot before saving.
 */
public record RescheduleAppointmentRequest(
        @NotNull(message = "New start time is required")
        ZonedDateTime newStartTime,

        @NotBlank(message = "Reschedule reason is required")
        @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
        String reason,

        Boolean allowConflictOverride
) {
    /**
     * Compact constructor for validation and defaults.
     */
    public RescheduleAppointmentRequest {
        // Default conflict override to false if not provided
        if (allowConflictOverride == null) {
            allowConflictOverride = false;
        }
    }
}
