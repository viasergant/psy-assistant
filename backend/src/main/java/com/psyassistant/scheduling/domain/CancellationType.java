package com.psyassistant.scheduling.domain;

/**
 * Classification of appointment cancellation initiator.
 *
 * <p>Tracks who initiated the cancellation for business analytics and rescheduling policies.
 */
public enum CancellationType {
    /**
     * Client initiated the cancellation request.
     */
    CLIENT_INITIATED,

    /**
     * Therapist initiated the cancellation (e.g., emergency, illness).
     */
    THERAPIST_INITIATED,

    /**
     * Cancellation occurred within the late cancellation window (e.g., < 24 hours notice).
     * May incur cancellation fee per business policy.
     */
    LATE_CANCELLATION
}
