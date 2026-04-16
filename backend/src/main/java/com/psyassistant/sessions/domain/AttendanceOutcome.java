package com.psyassistant.sessions.domain;

/**
 * Attendance outcome for a session record.
 *
 * <p>Mirrors the Postgres native enum {@code attendance_outcome_type}.
 */
public enum AttendanceOutcome {
    /** Client attended the session. */
    ATTENDED,

    /** Client did not attend and did not cancel in advance. */
    NO_SHOW,

    /**
     * Client cancelled within the organisation's late-cancellation window.
     * Automatically downgraded to {@link #CANCELLED} if the cancellation
     * timestamp falls outside the configured window.
     */
    LATE_CANCELLATION,

    /** Client cancelled with sufficient notice (outside the late-cancellation window). */
    CANCELLED,

    /**
     * Therapist or administrator cancelled the session.
     * Does not trigger client at-risk logic.
     */
    THERAPIST_CANCELLATION
}
