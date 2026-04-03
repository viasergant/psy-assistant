package com.psyassistant.sessions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * Request to complete an in-progress session with clinical notes and optional actual end time.
 *
 * @param sessionNotes clinical notes entered by therapist (required, min 10 characters)
 * @param actualEndTime actual end time if different from scheduled (optional)
 */
public record CompleteSessionRequest(
        @NotBlank(message = "Session notes are required")
        @Size(min = 10, message = "Session notes must be at least 10 characters")
        String sessionNotes,

        LocalTime actualEndTime
) {
}
