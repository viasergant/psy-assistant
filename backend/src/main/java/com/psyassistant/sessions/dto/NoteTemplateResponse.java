package com.psyassistant.sessions.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a note template listing.
 */
public record NoteTemplateResponse(
    UUID id,
    String name,
    String description,
    List<NoteTemplateFieldResponse> fields,
    Instant createdAt
) { }
