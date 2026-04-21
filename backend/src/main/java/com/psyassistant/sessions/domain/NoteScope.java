package com.psyassistant.sessions.domain;

/**
 * Scope discriminator for session notes.
 *
 * <p>Used to distinguish shared group-level notes from per-client private notes
 * within a GROUP session record.
 *
 * <p>Mirrors the Postgres native enum {@code note_scope}.
 */
public enum NoteScope {

    /**
     * Note is shared at the session level.
     * For GROUP sessions, it is accessible (subject to visibility rules) to all linked clients.
     * For INDIVIDUAL sessions, this is the only meaningful scope.
     */
    SESSION,

    /**
     * Note is scoped to a specific client within the session.
     * {@link SessionNote#getTargetClientId()} is non-null when scope is CLIENT.
     * Only visible in that client's timeline; not shown to other group participants.
     */
    CLIENT
}
