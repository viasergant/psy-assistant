package com.psyassistant.notifications;

/**
 * Notification event types that may trigger an outbound email.
 */
public enum NotificationEventType {

    /** Reminder sent to a client before a scheduled appointment. */
    APPOINTMENT_REMINDER,

    /** Welcome message sent after a new client account is created. */
    WELCOME,

    /** Invoice has been issued and is ready for the client. */
    INVOICE_ISSUED,

    /** Password-reset link for an account holder. */
    PASSWORD_RESET,

    /** Account has been locked due to too many consecutive failed login attempts. */
    ACCOUNT_LOCKED,

    /** Client's prepaid session package has been fully used. */
    PACKAGE_EXHAUSTED,

    /** Client's prepaid session package has expired with unused sessions. */
    PACKAGE_EXPIRED,

    /** Reminder sent to a client about a cancelled appointment. */
    APPOINTMENT_CANCELLATION,

    /** Follow-up sent to a client after a no-show appointment. */
    NO_SHOW_FOLLOWUP,

    /** Link to confirm the email address used for self-service registration. */
    EMAIL_CONFIRMATION,

    /** Notice to an existing user that someone tried to register with their address. */
    REGISTRATION_ATTEMPT_EXISTING
}
