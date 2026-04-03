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
    CONFLICT_OVERRIDE,

    // ========== Recurring Series Actions (PA-33) ==========

    /**
     * A new recurring appointment series was created with one or more occurrences.
     */
    SERIES_CREATED,

    /**
     * All (remaining) occurrences of a series were cancelled atomically.
     */
    SERIES_CANCELLED,

    /**
     * A single occurrence within a series was edited (isModified flagged).
     */
    SERIES_OCCURRENCE_EDITED,

    /**
     * This occurrence and all future occurrences in the series were edited.
     */
    SERIES_FUTURE_EDITED
}
