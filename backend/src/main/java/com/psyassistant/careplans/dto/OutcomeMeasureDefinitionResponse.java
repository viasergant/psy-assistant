package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.AlertSeverity;
import java.util.UUID;

/** Response DTO for an outcome measure definition. */
public record OutcomeMeasureDefinitionResponse(
    UUID id,
    String code,
    String displayName,
    String description,
    int minScore,
    int maxScore,
    Integer alertThreshold,
    String alertLabel,
    AlertSeverity alertSeverity,
    short sortOrder
) { }
