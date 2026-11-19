package com.psyassistant.common.mode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for self-service solo therapist registration.
 *
 * <p>Creates a new {@link com.psyassistant.organization.Organization} and an owner
 * {@link com.psyassistant.users.User} in a single transaction.
 *
 * <p>Only active in the {@code saas} Spring profile via
 * {@link RegistrationController}.
 */
public record RegistrationRequest(

        @NotBlank
        @Size(max = 255)
        String orgName,

        @NotBlank
        @Email
        @Size(max = 255)
        String ownerEmail,

        @NotBlank
        @Size(min = 8, max = 255)
        String password
) {
}
