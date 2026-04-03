package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.ConflictResolution;
import com.psyassistant.scheduling.domain.EditScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

/**
 * Request DTO for editing a single occurrence or all future occurrences of a series.
 *
 * <p>At least one of {@code startTime} or {@code durationMinutes} must be non-null.
 */
public record EditRecurringOccurrenceRequest(
        /** Whether to apply the change to SINGLE or FUTURE_SERIES. */
        @NotNull(message = "Edit scope is required")
        EditScope editScope,

        /** New start time for the occurrence(s), or null to keep existing. */
        ZonedDateTime startTime,

        /** New duration in minutes, or null to keep existing. */
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Max(value = 480, message = "Duration cannot exceed 8 hours")
        Integer durationMinutes,

        String notes,

        /** How to handle conflicts when rescheduling multiple future occurrences. */
        ConflictResolution conflictResolution
) {
    public EditRecurringOccurrenceRequest {
        if (startTime == null && durationMinutes == null && notes == null) {
            throw new IllegalArgumentException(
                    "At least one of startTime, durationMinutes, or notes must be provided");
        }
        if (durationMinutes != null && durationMinutes % 15 != 0) {
            throw new IllegalArgumentException("Duration must be a multiple of 15 minutes");
        }
        if (conflictResolution == null) {
            conflictResolution = ConflictResolution.ABORT;
        }
        if (editScope == EditScope.ENTIRE_SERIES) {
            throw new IllegalArgumentException(
                    "ENTIRE_SERIES edit scope is not supported via this endpoint");
        }
    }
}
