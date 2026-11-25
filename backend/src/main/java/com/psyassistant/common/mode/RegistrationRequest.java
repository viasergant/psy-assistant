package com.psyassistant.common.mode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for self-service solo therapist registration.
 *
 * <p>Creates a new {@link com.psyassistant.organization.Organization} and an owner
 * {@link com.psyassistant.users.User} (inactive until the email is confirmed).
 *
 * <p>Only active in the {@code saas} Spring profile via
 * {@link RegistrationController}.
 *
 * @param orgName      display name of the new organization
 * @param ownerEmail   owner's email address
 * @param password     owner's password
 * @param captchaToken CAPTCHA widget response (required when CAPTCHA is enabled)
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
        String password,

        @Size(max = 4096)
        String captchaToken
) {
}
