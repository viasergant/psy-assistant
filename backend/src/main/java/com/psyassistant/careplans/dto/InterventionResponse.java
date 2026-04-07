package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.InterventionStatus;
import java.time.Instant;
import java.util.UUID;

/** Intervention nested inside {@link GoalResponse}. */
public record InterventionResponse(
    UUID id,
    String interventionType,
    String description,
    String frequency,
    InterventionStatus status,
    Instant createdAt,
    Instant updatedAt
) { }
