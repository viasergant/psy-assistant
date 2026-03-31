package com.psyassistant.scheduling.domain;

/**
 * Audit action classification for appointment changes.
 *
 * <p>Each action type represents a significant event in the appointment lifecycle
 * that must be tracked for compliance and business analytics.
 */
public enum AuditActionType {
    /**
     * New appointment was created.
     */
    CREATED,

    /**
     * Appointment was moved to a different date/time.
     */
    RESCHEDULED,

    /**
     * Appointment was cancelled.
     */
    CANCELLED,

    /**
     * Appointment status changed (e.g., SCHEDULED → CONFIRMED, CONFIRMED → COMPLETED).
     */
    STATUS_CHANGED,

    /**
     * Appointment notes or metadata were updated.
     */
    NOTES_UPDATED,

    /**
     * Appointment was created despite a detected conflict (requires override permission).
     */
    CONFLICT_OVERRIDE
}
