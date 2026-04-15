package com.psyassistant.users;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
