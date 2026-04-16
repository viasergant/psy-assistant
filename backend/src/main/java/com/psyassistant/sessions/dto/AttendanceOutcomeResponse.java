package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO returned after recording an attendance outcome.
 *
 * @param sessionId        session record UUID
 * @param effectiveOutcome the outcome that was actually stored (may differ from requested
 *                         when a LATE_CANCELLATION is downgraded to CANCELLED)
 * @param requestedOutcome the outcome that was originally requested
 * @param cancelledAt      cancellation timestamp (null for non-cancellation outcomes)
 * @param updatedAt        when the session record was last updated
 */
public record AttendanceOutcomeResponse(
        UUID sessionId,
        AttendanceOutcome effectiveOutcome,
        AttendanceOutcome requestedOutcome,
        Instant cancelledAt,
        Instant updatedAt
) {
}
