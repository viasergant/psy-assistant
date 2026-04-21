package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.NoteScope;
import com.psyassistant.sessions.domain.NoteType;
import com.psyassistant.sessions.domain.NoteVisibility;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * Request payload for creating a new session note.
 *
 * <p>For GROUP session records:
 * <ul>
 *     <li>Omit {@code noteScope} or set to {@code SESSION} for shared group-level notes.</li>
 *     <li>Set {@code noteScope = CLIENT} and supply {@code targetClientId} for
 *         per-client private notes.</li>
 * </ul>
 */
public record CreateNoteRequest(

    @NotNull(message = "noteType is required")
    NoteType noteType,

    @NotNull(message = "visibility is required")
    NoteVisibility visibility,

    /** Rich-text HTML content (required for FREE_FORM notes). */
    String content,

    /** Structured field values keyed by template field key (required for STRUCTURED notes). */
    Map<String, String> structuredFields,

    /**
     * Note scope: SESSION (default, shared) or CLIENT (per-client private).
     * Only meaningful for GROUP session records. Defaults to SESSION when null.
     */
    NoteScope noteScope,

    /**
     * Target client UUID for CLIENT-scoped notes within a GROUP session.
     * Ignored when noteScope is SESSION or null.
     */
    UUID targetClientId
) { }
