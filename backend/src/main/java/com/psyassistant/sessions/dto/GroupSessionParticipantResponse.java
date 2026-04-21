package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single participant in a group session.
 *
 * <p>Combines identity information with the per-client attendance outcome (if recorded).
 */
public record GroupSessionParticipantResponse(
        UUID participantId,
        UUID clientId,
        String clientName,
        Instant joinedAt,
        Instant removedAt,
        AttendanceOutcome attendanceOutcome
) {
}
