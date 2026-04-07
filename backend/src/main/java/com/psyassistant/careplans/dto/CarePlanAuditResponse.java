package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.AuditActionType;
import java.time.Instant;
import java.util.UUID;

/** One audit trail entry for a care plan. */
public record CarePlanAuditResponse(
    UUID id,
    UUID carePlanId,
    String entityType,
    UUID entityId,
    AuditActionType actionType,
    UUID actorUserId,
    String actorName,
    Instant actionTimestamp,
    String fieldName,
    String oldValue,
    String newValue
) { }
