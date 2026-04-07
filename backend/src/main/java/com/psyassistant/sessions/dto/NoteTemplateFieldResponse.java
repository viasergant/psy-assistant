package com.psyassistant.sessions.dto;

/**
 * A single field definition within a note template.
 */
public record NoteTemplateFieldResponse(
    String key,
    String label,
    boolean required
) { }
