package com.psyassistant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/password-reset/confirm}.
 *
 * @param token       the raw 64-char hex token from the reset email link
 * @param newPassword the new password submitted by the user
 */
public record PasswordResetConfirmDto(
        @NotBlank(message = "Token must not be blank")
        String token,

        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {
}
