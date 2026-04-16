package com.psyassistant.sessions.domain;

/**
 * Status values for a {@link FollowUpTask}.
 */
public enum FollowUpTaskStatus {
    /** Task is open and awaiting action. */
    PENDING,
    /** Task has been actioned and closed. */
    COMPLETED,
    /** Task was dismissed without action. */
    DISMISSED
}
