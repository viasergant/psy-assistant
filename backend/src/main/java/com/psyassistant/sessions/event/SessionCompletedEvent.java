package com.psyassistant.sessions.event;

import com.psyassistant.scheduling.domain.SessionType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Event published when a session transitions to COMPLETED status.
 *
 * <p>Used to trigger downstream side-effects such as automatic invoice generation
 * without creating a compile-time dependency from the sessions package on billing.
 */
public record SessionCompletedEvent(
        UUID sessionId,
        UUID clientId,
        UUID therapistId,
        LocalDate sessionDate,
        SessionType sessionType
) { }
