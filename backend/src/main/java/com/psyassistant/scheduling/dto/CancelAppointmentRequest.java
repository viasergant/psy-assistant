package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.CancellationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for cancelling an appointment.
 *
 * <p>Requires classification of who initiated the cancellation and a reason.
 * Once cancelled, the time slot becomes available for new bookings.
 */
public record CancelAppointmentRequest(
        @NotNull(message = "Cancellation type is required")
        CancellationType cancellationType,

        @NotBlank(message = "Cancellation reason is required")
        @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
        String reason
) {
}
