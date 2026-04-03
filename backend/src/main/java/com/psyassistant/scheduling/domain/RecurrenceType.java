package com.psyassistant.scheduling.domain;

/**
 * Defines how recurring appointment occurrences are spaced in time.
 */
public enum RecurrenceType {
    /** One occurrence per week, same wall-clock time. */
    WEEKLY,

    /** One occurrence every two weeks, same wall-clock time. */
    BIWEEKLY,

    /** One occurrence per month on the same calendar day, same wall-clock time. */
    MONTHLY
}
