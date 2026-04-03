package com.psyassistant.scheduling.domain;

/**
 * Scope of an edit or cancellation operation on a recurring series.
 *
 * <p>Used in the audit trail ({@code edit_scope} column) to record exactly
 * how many occurrences were affected by a staff action.
 */
public enum EditScope {
    /** Only the selected occurrence is affected. */
    SINGLE,

    /** The selected occurrence and all future occurrences in the series are affected. */
    FUTURE_SERIES,

    /** Every occurrence in the series (past and future) is affected. */
    ENTIRE_SERIES
}
