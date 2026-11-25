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
        OrganizationType orgType) {

    /**
     * Maps a user and its (possibly null) organization to the DTO.
     *
     * @param user source entity
     * @param org  owning organization, or null if not resolvable
     * @return populated DTO
     */
    public static GlobalUserSummaryDto from(final User user, final Organization org) {
        return new GlobalUserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                user.isActive(),
                user.getCreatedAt(),
                user.getOrgId(),
                org == null ? null : org.getName(),
                org == null ? null : org.getType());
    }
}
