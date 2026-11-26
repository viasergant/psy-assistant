package com.psyassistant.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /api/v1/users/me/password.
 *
 * <p>Both fields are capped at 72 characters: BCrypt ignores input beyond 72 bytes and
 * unbounded input would be a cheap DoS vector.
 *
 * @param currentPassword the user's current password
 * @param newPassword     the desired new password (complexity is checked by the service)
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password must not exceed 72 characters")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(max = 72, message = "New password must not exceed 72 characters")
        String newPassword
) {
}
