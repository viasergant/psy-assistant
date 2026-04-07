package com.psyassistant.careplans.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Request payload for adding or updating a milestone under a goal. */
public record CreateMilestoneRequest(
    @NotBlank String description,
    LocalDate targetDate
) { }
