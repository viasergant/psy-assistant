package com.psyassistant.careplans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for adding or updating an intervention under a goal. */
public record CreateInterventionRequest(
    @NotBlank @Size(max = 100) String interventionType,
    @NotBlank String description,
    @Size(max = 100) String frequency
) { }
