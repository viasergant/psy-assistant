package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.NoteVisibility;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a single historic version of a session note.
 */
public record NoteVersionResponse(
    UUID id,
    int versionNumber,
    /** Decrypted rich-text HTML snapshot (FREE_FORM notes); null for STRUCTURED. */
    String content,
    /** Deserialized structured field snapshot (STRUCTURED notes); null for FREE_FORM. */
    Map<String, String> structuredFields,
    /** Display name of the author who wrote this version. */
    String authorName,
    NoteVisibility visibility,
    Instant createdAt
) { }
