package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request body for recording a per-client attendance outcome in a group session.
 *
 * <p>The target client UUID is supplied as a path parameter; this body carries
 * the outcome value plus optional late-cancellation metadata.
 */
public record RecordGroupAttendanceRequest(
        @NotNull AttendanceOutcome outcome,
        Instant cancelledAt,
        String note
) {
}
