package com.psyassistant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/password-reset/request}.
 *
 * @param email the email address of the account whose password should be reset
 */
public record PasswordResetRequestDto(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email
) {
}
