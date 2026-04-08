package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.AlertSeverity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Response DTO for a single recorded outcome measure entry. */
public record OutcomeMeasureEntryResponse(
    UUID id,
    String measureCode,
    String measureDisplayName,
    int score,
    LocalDate assessmentDate,
    String notes,
    boolean thresholdBreached,
    String alertLabel,
    AlertSeverity alertSeverity,
    String recordedByName,
    Instant createdAt
) { }
