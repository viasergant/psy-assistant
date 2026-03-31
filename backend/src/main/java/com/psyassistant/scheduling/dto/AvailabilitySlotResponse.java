package com.psyassistant.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO representing a single 30-minute availability slot.
 */
public record AvailabilitySlotResponse(
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String status  // "available", "booked" (future extension)
) {
}
