package com.psyassistant.scheduling.domain;

/**
 * Lifecycle status for an {@link AppointmentSeries}.
 */
public enum SeriesStatus {
    /** All generated occurrences are still active. */
    ACTIVE,

    /** One or more occurrences have been cancelled but others remain. */
    PARTIALLY_CANCELLED,

    /** All occurrences have been cancelled. */
    CANCELLED
}
