package com.psyassistant.users.dto;

import com.psyassistant.users.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Partial-update request for a user account.
 *
 * <p>All fields are optional; only non-null fields are applied.
 *
 * @param fullName new display name (1–255 characters); null means no change
 * @param roles    new set of roles; null means no change; must not be empty if provided
 * @param active   new active status; null means no change
 */
public record PatchUserRequest(

        @Size(min = 1, max = 255, message = "fullName must be between 1 and 255 characters")
        String fullName,

        @Size(min = 1, message = "roles must not be empty if provided")
        Set<@NotNull UserRole> roles,

        Boolean active) {
}
