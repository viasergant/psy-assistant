package com.psyassistant.sessions.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to manually start a session from an appointment.
 *
 * <p>Used by the POST /api/sessions/start endpoint.
 */
public record StartSessionRequest(
        @NotNull(message = "Appointment ID is required")
        UUID appointmentId
) { }
