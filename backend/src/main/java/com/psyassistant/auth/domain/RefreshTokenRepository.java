package com.psyassistant.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link RefreshToken} entities.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a non-expired, non-revoked token by its SHA-256 hash.
     *
     * @param hash    SHA-256 hex of the raw token
     * @return optional token record
     */
    @Query("""
            SELECT t FROM RefreshToken t
            WHERE t.tokenHash = :hash
              AND t.revoked = false
              AND t.expiresAt > CURRENT_TIMESTAMP
            """)
    Optional<RefreshToken> findActiveByHash(@Param("hash") String hash);

    /**
     * Returns the count of active (non-revoked, non-expired) sessions for a user.
     *
     * @param userId the user's UUID
     * @return number of active sessions
     */
    @Query("""
            SELECT COUNT(t) FROM RefreshToken t
            WHERE t.user.id = :userId
              AND t.revoked = false
              AND t.expiresAt > CURRENT_TIMESTAMP
            """)
    long countActiveSessions(@Param("userId") UUID userId);

    /**
     * Returns the oldest active session for a user (used for session cap eviction).
     *
     * @param userId the user's UUID
     * @return list containing the oldest active token, if any
     */
    @Query("""
            SELECT t FROM RefreshToken t
            WHERE t.user.id = :userId
              AND t.revoked = false
              AND t.expiresAt > CURRENT_TIMESTAMP
            ORDER BY t.createdAt ASC
            LIMIT 1
            """)
    List<RefreshToken> findOldestActiveByUserId(@Param("userId") UUID userId);

    /**
     * Deletes all tokens (active or not) belonging to a user.
     * Used for ADMIN single-session eviction and replay-attack mitigation.
     *
     * @param userId the user's UUID
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
