package com.psyassistant.users.dto;

import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.util.UUID;

/**
 * Response payload returned after successfully creating a new user account.
 *
 * <p>The {@code temporaryPassword} field contains the auto-generated password
 * for the new account. This value is shown once and never stored server-side
 * in plain text. The admin must securely copy and share it with the new user.
 *
 * @param id                user primary key
 * @param email             unique email address
 * @param fullName          display name
 * @param role              assigned role
 * @param temporaryPassword plain-text password (returned only on creation)
 */
public record UserCreationResponseDto(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        String temporaryPassword) {

    /**
     * Maps a {@link User} entity and temporary password to a creation response DTO.
     *
     * @param user              source entity
     * @param temporaryPassword the auto-generated plain-text password
     * @return populated DTO
     */
    public static UserCreationResponseDto from(final User user, final String temporaryPassword) {
        return new UserCreationResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                temporaryPassword
        );
    }
}
