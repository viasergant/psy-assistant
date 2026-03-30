package com.psyassistant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /api/v1/auth/first-login-password-change.
 *
 * <p>Used when a user with {@code mustChangePassword=true} needs to set a permanent
 * password before accessing the system. The {@code currentPassword} must match the
 * temporary password that was provided to the user at account creation.
 *
 * <p>Both password fields are capped at 72 characters to prevent BCrypt DoS attacks
 * (BCrypt silently truncates inputs beyond 72 bytes anyway).
 *
 * @param currentPassword the temporary password provided at account creation
 * @param newPassword     the new permanent password chosen by the user
 */
public record FirstLoginPasswordChangeDto(

        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password must not exceed 72 characters")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 10, max = 72, message = "New password must be between 10 and 72 characters")
        String newPassword
) {
}
