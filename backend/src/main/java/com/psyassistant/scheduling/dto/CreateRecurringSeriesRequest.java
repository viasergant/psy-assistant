package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.ConflictResolution;
import com.psyassistant.scheduling.domain.RecurrenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a new recurring appointment series.
 *
 * <p>The caller must first call {@code POST /check-conflicts} to preview conflicts,
 * then submit this request with an explicit {@link ConflictResolution} choice.
 */
public record CreateRecurringSeriesRequest(
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

        @NotNull(message = "Recurrence type is required")
        RecurrenceType recurrenceType,

        @NotNull(message = "Occurrence count is required")
        @Min(value = 1, message = "At least one occurrence is required")
        @Max(value = 20, message = "Maximum 20 occurrences allowed")
        Integer occurrences,

        String notes,

        /**
         * How to handle conflicting slots: SKIP_CONFLICTS (save clean ones, waitlist rest)
         * or ABORT (save nothing).
         */
        @NotNull(message = "Conflict resolution strategy is required")
        ConflictResolution conflictResolution
) {
    public CreateRecurringSeriesRequest {
        if (durationMinutes != null && durationMinutes % 15 != 0) {
            throw new IllegalArgumentException("Duration must be a multiple of 15 minutes");
        }
        if (conflictResolution == null) {
            conflictResolution = ConflictResolution.ABORT;
        }
    }
}
