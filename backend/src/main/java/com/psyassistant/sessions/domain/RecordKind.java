package com.psyassistant.sessions.domain;

/**
 * Discriminator for session record type.
 *
 * <p>Immutable after creation — enforced at JPA layer ({@code updatable = false})
 * and at application layer via constructor restriction.
 *
 * <p>Mirrors the Postgres native enum {@code record_kind}.
 */
public enum RecordKind {

    /**
     * Standard one-to-one session between a therapist and a single client.
     * {@link SessionRecord#getClientId()} is non-null for INDIVIDUAL records.
     */
    INDIVIDUAL,

    /**
     * Group therapy session between a therapist and multiple clients.
     * {@link SessionRecord#getClientId()} is null; participants are tracked
     * in the {@code session_participant} join table.
     */
    GROUP
}
