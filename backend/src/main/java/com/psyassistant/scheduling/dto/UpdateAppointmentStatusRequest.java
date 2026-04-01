package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update appointment status.
 *
 * <p>Used by the PATCH /api/v1/appointments/{id}/status endpoint.
 */
public record UpdateAppointmentStatusRequest(
        @NotNull(message = "Status is required")
        AppointmentStatus status,

        String notes
) { }
