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

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

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
        this.mustChangePassword = false;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Constructs a new user with a full name.
     *
     * @param email        unique email address
     * @param passwordHash BCrypt hash of the password
     * @param fullName     display name
     * @param role         user role
     * @param active       whether the account is active
     */
    public User(final String email, final String passwordHash, final String fullName,
                final UserRole role, final boolean active) {
        this(email, passwordHash, role, active);
        this.fullName = fullName;
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
     * Returns the user's full display name.
     *
     * @return full name or null if not set
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the user's full display name and updates the updatedAt timestamp.
     *
     * @param fullName new full name
     */
    public void setFullName(final String fullName) {
        this.fullName = fullName;
        this.updatedAt = Instant.now();
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
     * Sets the user's role and updates the updatedAt timestamp.
     *
     * @param role new role
     */
    public void setRole(final UserRole role) {
        this.role = role;
        this.updatedAt = Instant.now();
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
     * Sets the account active status and updates the updatedAt timestamp.
     *
     * @param active new status
     */
    public void setActive(final boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
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

    /**
     * Returns whether the user must change their password on next login.
     *
     * @return true if password change is required
     */
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /**
     * Sets whether the user must change their password on next login,
     * and updates the updatedAt timestamp.
     *
     * @param mustChangePassword whether password change is required
     */
    public void setMustChangePassword(final boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
        this.updatedAt = Instant.now();
    }

    /**
     * Updates the user's password hash and clears the mustChangePassword flag.
     * Updates the updatedAt timestamp.
     *
     * @param newPasswordHash new BCrypt password hash
     */
    public void updatePasswordHash(final String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
        this.updatedAt = Instant.now();
    }
}
