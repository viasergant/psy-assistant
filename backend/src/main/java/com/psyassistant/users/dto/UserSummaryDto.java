package com.psyassistant.users.dto;

import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only projection of a {@link User} account, returned in list and single-resource responses.
 *
 * @param id        user primary key
 * @param email     unique email address
 * @param fullName  display name (may be null for legacy accounts)
 * @param roles     all roles assigned to the user
 * @param role      first assigned role (deprecated — use {@code roles} instead)
 * @param active    true if the account is active
 * @param createdAt account creation timestamp
 * @param updatedAt last modification timestamp
 */
public record UserSummaryDto(
        UUID id,
        String email,
        String fullName,
        Set<UserRole> roles,
        @Deprecated UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps a {@link User} JPA entity to a summary DTO.
     *
     * @param user source entity
     * @return populated DTO
     */
    public static UserSummaryDto from(final User user) {
        Set<UserRole> userRoles = user.getRoles();
        UserRole firstRole = userRoles.isEmpty() ? null : userRoles.iterator().next();
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                userRoles,
                firstRole,
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
