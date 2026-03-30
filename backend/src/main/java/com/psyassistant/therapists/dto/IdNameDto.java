package com.psyassistant.therapists.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Simple DTO for reference data entities (e.g., Specialization, Language).
 * Contains just ID and name for dropdown/selection use cases.
 */
public record IdNameDto(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("name")
    String name
) {
    public static IdNameDto from(UUID id, String name) {
        return new IdNameDto(id, name);
    }
}
