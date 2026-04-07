package com.psyassistant.sessions.domain;

/**
 * Controls which roles can read a session note.
 *
 * <ul>
 *   <li>{@link #PRIVATE} — visible only to the authoring therapist</li>
 *   <li>{@link #SUPERVISOR_VISIBLE} — visible to the author and to supervisors / administrators</li>
 * </ul>
 */
public enum NoteVisibility {

    /** Note is visible only to the authoring therapist. */
    PRIVATE,

    /** Note is visible to the author and to supervisors / system administrators. */
    SUPERVISOR_VISIBLE
}
