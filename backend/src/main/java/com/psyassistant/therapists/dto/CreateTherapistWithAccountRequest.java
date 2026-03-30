package com.psyassistant.therapists.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for creating a therapist user account and profile atomically.
 * This creates both the User entity with a temporary password and the TherapistProfile entity.
 */
public record CreateTherapistWithAccountRequest(
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid address")
    @Size(max = 255, message = "email must not exceed 255 characters")
    @JsonProperty("email")
    String email,

    @NotBlank(message = "fullName is required")
    @Size(min = 1, max = 255, message = "fullName must be between 1 and 255 characters")
    @JsonProperty("fullName")
    String fullName,

    @JsonProperty("phone")
    String phone,

    @NotBlank(message = "employmentStatus is required")
    @JsonProperty("employmentStatus")
    String employmentStatus,

    @NotNull(message = "primarySpecializationId is required")
    @JsonProperty("primarySpecializationId")
    UUID primarySpecializationId
) { }
