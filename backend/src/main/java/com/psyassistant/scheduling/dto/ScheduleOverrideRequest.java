package com.psyassistant.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating or updating a schedule override.
 */
public record ScheduleOverrideRequest(

    @NotNull(message = "Override date is required")
    LocalDate overrideDate,

    @NotNull(message = "Availability flag is required")
    Boolean isAvailable,

    LocalTime startTime,  // Required if isAvailable = true

    LocalTime endTime,    // Required if isAvailable = true

    String reason         // Optional explanation
) {
}
