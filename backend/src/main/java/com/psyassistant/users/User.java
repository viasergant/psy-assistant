package com.psyassistant.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an internal CRM user.
 *
 * <p>Password is stored as a BCrypt hash (strength 12). Plain-text passwords are
 * never stored and must never appear in logs.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected User() {
    }

    /**
     * Constructs a new user.
     *
     * @param email        unique email address
     * @param passwordHash BCrypt hash of the password
     * @param role         user role
     * @param active       whether the account is active
     */
    public User(final String email, final String passwordHash,
                final UserRole role, final boolean active) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the user's surrogate primary key.
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the user's email address.
     *
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the BCrypt-hashed password.
     *
     * @return password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Returns the user's role.
     *
     * @return role
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Returns whether the account is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns when the user record was created.
     *
     * @return creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns when the user record was last updated.
     *
     * @return last-update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
