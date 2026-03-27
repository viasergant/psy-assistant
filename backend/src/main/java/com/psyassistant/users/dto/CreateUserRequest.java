package com.psyassistant.users.dto;

import com.psyassistant.users.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new internal user account.
 *
 * @param email    unique, valid email address
 * @param fullName display name (1–255 characters)
 * @param role     role to assign; must be a valid {@link UserRole} value
 */
public record CreateUserRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,

        @NotBlank(message = "fullName is required")
        @Size(min = 1, max = 255, message = "fullName must be between 1 and 255 characters")
        String fullName,

        @NotNull(message = "role is required")
        UserRole role) {
}
