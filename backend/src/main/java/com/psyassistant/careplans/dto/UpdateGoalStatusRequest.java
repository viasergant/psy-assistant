package com.psyassistant.careplans.dto;

import com.psyassistant.careplans.domain.GoalStatus;
import jakarta.validation.constraints.NotNull;

/** Request payload to patch goal status. */
public record UpdateGoalStatusRequest(@NotNull GoalStatus status) { }
