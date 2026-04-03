package com.psyassistant.sessions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to cancel a pending or in-progress session with a reason.
 *
 * @param reason cancellation reason (required)
 */
public record CancelSessionRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(min = 3, max = 1000, message = "Cancellation reason must be between 3 and 1000 characters")
        String reason
) {
}
