package com.psyassistant.common.mode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the clinic initial-admin setup endpoint.
 *
 * <p>All fields except {@code fullName} are required. {@code password} must be at least
 * 8 characters; further complexity enforcement is left to the caller's environment.
 *
 * @param email    valid, non-blank email address for the admin account
 * @param password plain-text password (minimum 8 characters); hashed before persistence
 * @param fullName optional display name
 */
public record ClinicSetupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String fullName) {
}
