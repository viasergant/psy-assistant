package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.GoalStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Goal detail nested inside {@link CarePlanDetailResponse}. */
public record GoalResponse(
    UUID id,
    String description,
    short priority,
    LocalDate targetDate,
    GoalStatus status,
    Instant createdAt,
    Instant updatedAt,
    List<InterventionResponse> interventions,
    List<MilestoneResponse> milestones
) { }
