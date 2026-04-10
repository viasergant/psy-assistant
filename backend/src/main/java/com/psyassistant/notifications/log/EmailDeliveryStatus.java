package com.psyassistant.notifications.log;

/**
 * Lifecycle states of an outbound email delivery attempt tracked in the outbox.
 */
public enum EmailDeliveryStatus {

    /** Written by the domain layer; not yet picked up by the poller. */
    PENDING,

    /** Currently being dispatched by {@code EmailOutboxProcessor}. */
    SENDING,

    /** Successfully accepted by the SMTP provider. */
    SENT,

    /** Transient failure; scheduled for retry by the poller. */
    RETRY,

    /** All retry attempts exhausted; permanent delivery failure. */
    FAILED,

    /** Provider delivery rejected the message (hard or soft bounce). */
    BOUNCED,

    /** SMTP credentials or sender address are missing; no send attempt was made. */
    CONFIG_ERROR
}
