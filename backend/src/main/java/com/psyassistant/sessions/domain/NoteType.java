package com.psyassistant.sessions.domain;

/**
 * Discriminates between free-form (rich text) and structured (template-based) session notes.
 */
public enum NoteType {

    /** Rich-text HTML note authored freely by the therapist. */
    FREE_FORM,

    /** Template-driven note with fixed named fields (e.g. SOAP). */
    STRUCTURED
}
