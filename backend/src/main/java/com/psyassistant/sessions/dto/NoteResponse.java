package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.NoteScope;
import com.psyassistant.sessions.domain.NoteType;
import com.psyassistant.sessions.domain.NoteVisibility;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a session note (current version).
 */
public record NoteResponse(
    UUID id,
    UUID sessionRecordId,
    NoteType noteType,
    NoteVisibility visibility,
    /** Decrypted rich-text HTML (FREE_FORM notes); null for STRUCTURED. */
    String content,
    /** Deserialized structured fields (STRUCTURED notes); null for FREE_FORM. */
    Map<String, String> structuredFields,
    /** Display name of the note author. */
    String authorName,
    Instant createdAt,
    Instant updatedAt,
    /** True if at least one previous version exists. */
    boolean hasVersionHistory,
    /** Note scope: SESSION (shared) or CLIENT (per-client private). */
    NoteScope noteScope,
    /** Target client UUID for CLIENT-scoped notes; null for SESSION-scoped notes. */
    UUID targetClientId
) { }
