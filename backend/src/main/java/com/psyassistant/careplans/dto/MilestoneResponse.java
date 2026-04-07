package com.psyassistant.careplans.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Milestone nested inside {@link GoalResponse}. */
public record MilestoneResponse(
    UUID id,
    String description,
    LocalDate targetDate,
    Instant achievedAt,
    UUID achievedByUserId,
    Instant createdAt,
    Instant updatedAt
) { }
