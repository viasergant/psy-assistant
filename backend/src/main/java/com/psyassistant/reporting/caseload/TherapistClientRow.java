package com.psyassistant.reporting.caseload;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single row in the per-therapist client drill-down list.
 *
 * <p>This DTO intentionally excludes all clinical data (session notes, care plan details,
 * financial information) — it is scoped to administrative scheduling visibility only.
 *
 * @param clientId              UUID of the client record
 * @param clientName            full display name of the client
 * @param completedSessionCount number of COMPLETED session records for this
 *                              therapist-client pair
 * @param nextScheduledSession  earliest upcoming SCHEDULED or CONFIRMED appointment
 *                              (null if none)
 * @param clientStatus          treatment status: ACTIVE, ON_HOLD, or DISCHARGED
 */
public record TherapistClientRow(
    UUID clientId,
    String clientName,
    int completedSessionCount,
    Instant nextScheduledSession,
    String clientStatus
) {
}
