package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.CarePlanStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full detail response for a single care plan. */
public record CarePlanDetailResponse(
    UUID id,
    UUID clientId,
    UUID therapistId,
    String title,
    String description,
    CarePlanStatus status,
    Instant closedAt,
    UUID closedByUserId,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt,
    String createdBy,
    List<GoalResponse> goals
) { }
