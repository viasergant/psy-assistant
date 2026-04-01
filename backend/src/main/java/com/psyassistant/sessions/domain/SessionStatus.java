package com.psyassistant.sessions.domain;

/**
 * Session record lifecycle status.
 *
 * <p>Represents the current state of a session record throughout its lifecycle.
 */
public enum SessionStatus {
    /**
     * Session record created but session has not started yet.
     * Typically created when appointment is marked as completed.
     */
    PENDING,

    /**
     * Session is currently in progress (therapist has started the session).
     * Created when therapist manually starts a session from an appointment.
     */
    IN_PROGRESS,

    /**
     * Session has been completed and all documentation is finalized.
     */
    COMPLETED,

    /**
     * Session was cancelled (typically due to appointment cancellation).
     */
    CANCELLED
}
