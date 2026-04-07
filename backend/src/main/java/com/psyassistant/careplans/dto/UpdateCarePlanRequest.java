package com.psyassistant.careplans.dto;

import jakarta.validation.constraints.Size;

/** Request payload to update care plan header fields. */
public record UpdateCarePlanRequest(
    @Size(max = 255) String title,
    String description
) { }
