package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.NoteVisibility;
import java.util.Map;

/**
 * Request payload for updating an existing session note.
 *
 * <p>Only {@code visibility}, {@code content}, and {@code structuredFields} can change —
 * note type is immutable after creation.
 */
public record UpdateNoteRequest(

    NoteVisibility visibility,

    /** Updated rich-text HTML (FREE_FORM notes). */
    String content,

    /** Updated structured field map (STRUCTURED notes). */
    Map<String, String> structuredFields
) { }
