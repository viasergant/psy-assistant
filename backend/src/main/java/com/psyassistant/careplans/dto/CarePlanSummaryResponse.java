package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.CarePlanStatus;
import java.time.Instant;
import java.util.UUID;

/** Response body for a care plan summary (used in list endpoint). */
public record CarePlanSummaryResponse(
    UUID id,
    UUID clientId,
    UUID therapistId,
    String title,
    String description,
    CarePlanStatus status,
    int goalCount,
    Instant createdAt,
    Instant updatedAt
) { }
