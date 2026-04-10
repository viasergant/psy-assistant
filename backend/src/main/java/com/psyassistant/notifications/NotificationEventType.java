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

    /** Client's prepaid session package has been fully used. */
    PACKAGE_EXHAUSTED,

    /** Client's prepaid session package has expired with unused sessions. */
    PACKAGE_EXPIRED
}
