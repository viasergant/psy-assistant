package com.psyassistant.therapists.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating a therapist profile (admin update).
 * Includes version for optimistic locking.
 */
public record UpdateTherapistProfileRequest(
    @JsonProperty("version")
    Long version,

    @JsonProperty("email")
    String email,

    @JsonProperty("name")
    String name,

    @JsonProperty("phone")
    String phone,

    @JsonProperty("employmentStatus")
    String employmentStatus,

    @JsonProperty("bio")
    String bio,

    @JsonProperty("specializationIds")
    List<UUID> specializationIds,

    @JsonProperty("languageIds")
    List<UUID> languageIds
) {}
