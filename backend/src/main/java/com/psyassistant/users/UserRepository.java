package com.psyassistant.users;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email to look up
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns true if a user with the given email exists.
     *
     * @param email email to check
     * @return true if an account with this email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Returns true if a user with the given email exists within the specified organization.
     * Use this for org-scoped uniqueness checks (e.g. when an admin adds a therapist to a clinic).
     *
     * @param email email to check
     * @param orgId organization to scope the check to
     * @return true if the email is already in use within that org
     */
    boolean existsByEmailAndOrgId(String email, UUID orgId);

    /**
     * Returns users who never confirmed their email and registered before the cut-off.
     *
     * @param cutoff registration instant before which unverified users are stale
     * @return stale unverified users
     */
    List<User> findAllByEmailVerifiedFalseAndCreatedAtBefore(Instant cutoff);

    /**
     * Counts the users belonging to an organization.
     *
     * @param orgId organization id
     * @return number of users
     */
    long countByOrgId(UUID orgId);

    /**
     * Atomically increments the failed login attempt counter for the given user.
     *
     * @param id the user's primary key
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedLoginAttempts(@Param("id") UUID id);

    /**
     * Resets the failed login attempt counter and clears any lockout for the given user.
     *
     * @param id the user's primary key
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
    void clearFailedLoginAttempts(@Param("id") UUID id);

    /**
     * Locks the account by setting lockedUntil and resetting the attempt counter.
     *
     * @param id        the user's primary key
     * @param lockedUntil timestamp until which the account is locked
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil, u.failedLoginAttempts = 0 WHERE u.id = :id")
    void lockAccount(@Param("id") UUID id, @Param("lockedUntil") Instant lockedUntil);

    /**
     * Unlocks the account by clearing lockedUntil and resetting the attempt counter.
     *
     * @param id the user's primary key
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = null, u.failedLoginAttempts = 0 WHERE u.id = :id")
    void unlockAccount(@Param("id") UUID id);

    /**
     * Reads only the {@code active} flag of a user (used to reject tokens of suspended users).
     *
     * @param id the user's UUID
     * @return the flag, or empty if the user does not exist
     */
    @Query("SELECT u.active FROM User u WHERE u.id = :id")
    Optional<Boolean> findActiveById(@Param("id") UUID id);

    /**
     * Records the last authenticated activity of a user without touching {@code updatedAt}.
     *
     * @param id  the user's UUID
     * @param now activity timestamp
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastSeenAt = :now WHERE u.id = :id")
    void touchLastSeen(@Param("id") UUID id, @Param("now") Instant now);

    /**
     * Counts all users.
     *
     * @return total number of users across all organizations
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    /**
     * Counts blocked users: suspended ({@code active = false}) or temporarily locked.
     *
     * @param now current instant
     * @return number of blocked users across all organizations
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = false OR u.lockedUntil > :now")
    long countBlocked(@Param("now") Instant now);

    /**
     * Counts online users: not blocked and active since {@code since}.
     *
     * @param now   current instant
     * @param since start of the online window
     * @return number of online users across all organizations
     */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.active = true
              AND (u.lockedUntil IS NULL OR u.lockedUntil <= :now)
              AND u.lastSeenAt >= :since
            """)
    long countOnline(@Param("now") Instant now, @Param("since") Instant since);
}
