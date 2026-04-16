package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request body for recording an attendance outcome on a session.
 *
 * @param outcome     the attendance outcome to record (required)
 * @param cancelledAt timestamp of the cancellation, required when outcome is
 *                    {@code LATE_CANCELLATION} or {@code CANCELLED} to evaluate
 *                    the late-cancellation policy window
 * @param note        optional free-text note recorded in the audit log
 */
public record RecordAttendanceRequest(
        @NotNull(message = "Attendance outcome is required")
        AttendanceOutcome outcome,

        Instant cancelledAt,

        String note
) {
}
