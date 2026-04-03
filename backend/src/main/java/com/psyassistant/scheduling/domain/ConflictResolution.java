package com.psyassistant.scheduling.domain;

/**
 * Staff's chosen resolution when conflicts are detected during recurring series creation.
 */
public enum ConflictResolution {
    /**
     * Save all non-conflicting occurrences; add conflicting slots to the waitlist
     * (publishes {@link com.psyassistant.scheduling.event.RecurringSlotSkippedEvent} for each skipped slot).
     */
    SKIP_CONFLICTS,

    /** Abort the entire operation — nothing is saved. */
    ABORT
}
