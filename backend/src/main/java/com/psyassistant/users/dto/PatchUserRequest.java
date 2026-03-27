package com.psyassistant.users.dto;

import com.psyassistant.users.UserRole;
import jakarta.validation.constraints.Size;

/**
 * Partial-update request for a user account.
 *
 * <p>All fields are optional; only non-null fields are applied.
 *
 * @param fullName new display name (1–255 characters); null means no change
 * @param role     new role; null means no change
 * @param active   new active status; null means no change
 */
public record PatchUserRequest(

        @Size(min = 1, max = 255, message = "fullName must be between 1 and 255 characters")
        String fullName,

        UserRole role,

        Boolean active) {
}
