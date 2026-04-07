package com.psyassistant.careplans.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request payload for creating a new care plan. */
public record CreateCarePlanRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotEmpty @Valid List<CreateGoalRequest> goals
) {
    public CreateCarePlanRequest {
        goals = goals == null ? List.of() : List.copyOf(goals);
    }
}
