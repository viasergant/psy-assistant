package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.domain.EditScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for cancelling a single occurrence or remaining series occurrences.
 *
 * <p>When {@code cancelScope} is {@code FUTURE_SERIES} or {@code ENTIRE_SERIES},
 * the cancellation is applied atomically via a bulk UPDATE query.
 */
public record CancelRecurringOccurrenceRequest(
        /** Scope: SINGLE, FUTURE_SERIES, or ENTIRE_SERIES. */
        @NotNull(message = "Cancel scope is required")
        EditScope cancelScope,

        @NotNull(message = "Cancellation reason is required")
        @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
        String cancellationReason,

        @NotNull(message = "Cancellation type is required")
        CancellationType cancellationType
) {
}
