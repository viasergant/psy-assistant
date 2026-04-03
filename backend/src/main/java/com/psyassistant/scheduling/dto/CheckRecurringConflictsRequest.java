package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.RecurrenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Request DTO for pre-flight recurring conflict checking.
 *
 * <p>Returns a slot-by-slot conflict map so the UI can present each
 * occurrence with its conflict status before the user commits.
 */
public record CheckRecurringConflictsRequest(
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
        Integer occurrences
) {
}
