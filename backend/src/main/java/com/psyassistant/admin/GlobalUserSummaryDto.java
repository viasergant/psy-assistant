package com.psyassistant.admin;

import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only projection of a user for the global administrator, including the owning organization.
 *
 * @param id        user primary key
 * @param email     unique email address
 * @param fullName  display name (may be null)
 * @param roles     all roles assigned to the user
 * @param active    true if the account is active
 * @param createdAt account creation timestamp
 * @param orgId     owning organization id (may be null for legacy rows)
 * @param orgName   owning organization name (null when unknown)
 * @param orgType   owning organization type (null when unknown)
 * @param presence  online / offline / blocked marker
 * @param blockReason why the user is blocked (null unless presence is BLOCKED)
 * @param lastSeenAt last authenticated activity (null if never seen)
 */
public record GlobalUserSummaryDto(
        UUID id,
        String email,
        String fullName,
        Set<UserRole> roles,
        boolean active,
        Instant createdAt,
        UUID orgId,
        String orgName,
        OrganizationType orgType,
        UserPresence presence,
        BlockReason blockReason,
        Instant lastSeenAt) {

    /**
     * Maps a user and its (possibly null) organization to the DTO.
     *
     * @param user source entity
     * @param org  owning organization, or null if not resolvable
     * @param presence computed presence of the user
     * @param blockReason reason for the block, or null
     * @return populated DTO
     */
    public static GlobalUserSummaryDto from(
            final User user, final Organization org, final UserPresence presence, final BlockReason blockReason) {
        return new GlobalUserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                user.isActive(),
                user.getCreatedAt(),
                user.getOrgId(),
                org == null ? null : org.getName(),
                org == null ? null : org.getType(),
                presence,
                blockReason,
                user.getLastSeenAt());
    }
}
