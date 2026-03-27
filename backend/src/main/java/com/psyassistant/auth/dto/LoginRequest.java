package com.psyassistant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /api/v1/auth/login.
 *
 * <p>The {@code password} field is capped at 72 characters to prevent BCrypt DoS
 * attacks (BCrypt silently truncates inputs beyond 72 bytes anyway).
 *
 * @param email    the user's email address
 * @param password the plain-text password (never logged)
 */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(max = 72)
        String password
) {
}
