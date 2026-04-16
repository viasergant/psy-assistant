package com.psyassistant.sessions.event;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an attendance outcome is recorded for a session.
 *
 * <p>Consumed by:
 * <ul>
 *     <li>{@link com.psyassistant.sessions.service.NoShowFollowUpService} — creates follow-up tasks</li>
 *     <li>{@link com.psyassistant.sessions.service.AtRiskEvaluationService} — updates at-risk flag</li>
 * </ul>
 *
 * @param sessionId        session record UUID
 * @param clientId         client UUID
 * @param therapistId      therapist UUID
 * @param newOutcome       the newly recorded attendance outcome
 * @param previousOutcome  the previous attendance outcome (may be null)
 * @param cancelledAt      timestamp when cancellation occurred (may be null)
 * @param actorUserId      UUID of the user who recorded the outcome
 */
public record AttendanceOutcomeRecordedEvent(
        UUID sessionId,
        UUID clientId,
        UUID therapistId,
        AttendanceOutcome newOutcome,
        AttendanceOutcome previousOutcome,
        Instant cancelledAt,
        UUID actorUserId
) {
}
