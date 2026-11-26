package com.psyassistant.scheduling.domain;

/**
 * Appointment lifecycle status.
 *
 * <p>Represents the current state of an appointment booking throughout its lifecycle.
 */
public enum AppointmentStatus {
    /**
     * Appointment has been created but not yet confirmed by the client.
     */
    SCHEDULED,

    /**
     * Client has confirmed attendance (manual confirmation step).
     */
    CONFIRMED,

    /**
     * Session is currently in progress (therapist has started the session).
     */
    IN_PROGRESS,

    /**
     * Session completed successfully.
     */
    COMPLETED,

    /**
     * Appointment was cancelled before the scheduled time.
     * See {@code cancellationType} for who initiated the cancellation.
     */
    CANCELLED,

    /**
     * Client did not attend the scheduled appointment without prior notice.
     */
    NO_SHOW,

    /**
     * Client declined the session themselves (e.g. via the Telegram bot). The session stays on the
     * calendar, highlighted, until the therapist cancels or reschedules it; its time slot is free for rebooking.
     */
    CLIENT_REJECTED
}
