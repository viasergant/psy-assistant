package com.psyassistant.careplans.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

/** Request payload for adding or updating a goal within a care plan. */
public record CreateGoalRequest(
    @NotBlank String description,
    short priority,
    LocalDate targetDate,
    @Valid List<CreateInterventionRequest> interventions,
    @Valid List<CreateMilestoneRequest> milestones
) {
    public CreateGoalRequest {
        interventions = interventions == null ? List.of() : List.copyOf(interventions);
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
    }
}
