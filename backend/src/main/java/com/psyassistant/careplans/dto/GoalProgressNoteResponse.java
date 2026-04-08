package com.psyassistant.careplans.dto;

import java.time.Instant;
import java.util.UUID;

/** Response DTO for a single goal progress note. */
public record GoalProgressNoteResponse(
    UUID id,
    UUID goalId,
    String noteText,
    String authorName,
    Instant createdAt
) { }
