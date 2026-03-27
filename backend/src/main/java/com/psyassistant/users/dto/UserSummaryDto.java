package com.psyassistant.users.dto;

import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a {@link User} account, returned in list and single-resource responses.
 *
 * @param id        user primary key
 * @param email     unique email address
 * @param fullName  display name (may be null for legacy accounts)
 * @param role      assigned role
 * @param active    true if the account is active
 * @param createdAt account creation timestamp
 * @param updatedAt last modification timestamp
 */
public record UserSummaryDto(
        UUID id,
        String email,
        String fullName,
        UserRole role,
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
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
