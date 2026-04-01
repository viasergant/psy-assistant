package com.psyassistant.crm.clients.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for client activity timeline event.
 * Represents a single event in the client's history (appointment, profile change, conversion, etc.).
 */
@Schema(description = "Client activity timeline event")
public record TimelineEventDto(
    @Schema(description = "Event unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID eventId,

    @Schema(description = "Event type", example = "APPOINTMENT", allowableValues = {"APPOINTMENT", "PROFILE_CHANGE", "CONVERSION", "NOTE", "PAYMENT", "COMMUNICATION"})
    String eventType,

    @Schema(description = "Event subtype", example = "appointment.scheduled")
    String eventSubtype,

    @Schema(description = "Event timestamp")
    OffsetDateTime eventTimestamp,

    @Schema(description = "Name of the person who created the event", example = "Dr. Anna Kowalski")
    String actorName,

    @Schema(description = "Event-specific data as JSON object")
    JsonNode eventData,

    @Schema(description = "Event creation timestamp")
    OffsetDateTime createdAt
) {
    // No static factory method needed - will be constructed from JDBC results
}
