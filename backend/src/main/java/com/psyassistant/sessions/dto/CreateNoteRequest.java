package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.NoteType;
import com.psyassistant.sessions.domain.NoteVisibility;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request payload for creating a new session note.
 */
public record CreateNoteRequest(

    @NotNull(message = "noteType is required")
    NoteType noteType,

    @NotNull(message = "visibility is required")
    NoteVisibility visibility,

    /** Rich-text HTML content (required for FREE_FORM notes). */
    String content,

    /** Structured field values keyed by template field key (required for STRUCTURED notes). */
    Map<String, String> structuredFields
) { }
