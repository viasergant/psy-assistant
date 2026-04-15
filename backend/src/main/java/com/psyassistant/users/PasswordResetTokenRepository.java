package com.psyassistant.users;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link PasswordResetToken} entities.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds a valid (not expired, not used) token by its hash.
     *
     * @param tokenHash SHA-256 hex of the raw token
     * @return token if found and valid
     */
    @Query("SELECT t FROM PasswordResetToken t "
            + "WHERE t.tokenHash = :hash "
            + "AND t.usedAt IS NULL "
            + "AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordResetToken> findActiveByHash(@Param("hash") String tokenHash);

    /**
     * Finds any token by its hash, regardless of used/expired state.
     * Used during confirmation to distinguish invalid vs expired tokens.
     *
     * @param tokenHash SHA-256 hex of the raw token
     * @return token if it exists
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Deletes all tokens (used or expired) for a given user — called before issuing a new one
     * to avoid token accumulation.
     *
     * @param userId the user whose tokens should be purged
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
