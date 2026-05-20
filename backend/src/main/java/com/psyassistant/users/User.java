package com.psyassistant.users;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an internal CRM user.
 *
 * <p>Password is stored as a BCrypt hash (strength 12). Plain-text passwords are
 * never stored and must never appear in logs.
 *
 * <p>A user may hold multiple roles simultaneously. The set is stored in the
 * {@code user_roles} junction table and loaded eagerly to avoid
 * {@link org.hibernate.LazyInitializationException} when roles are accessed outside
 * a JPA transaction (e.g. in {@code TokenService}).
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, length = 5)
    private String language;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** Required by JPA. */
    protected User() {
    }

    /**
     * Constructs a new user with a set of roles.
     *
     * @param email        unique email address
     * @param passwordHash BCrypt hash of the password
     * @param fullName     display name (may be null)
     * @param roles        non-empty set of roles; each element is canonicalised
     * @param active       whether the account is active
     */
    public User(final String email, final String passwordHash, final String fullName,
                final Set<UserRole> roles, final boolean active) {
        this.email = email;
        this.mustChangePassword = false;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
        this.language = "en"; // Default locale
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        this.roles = new LinkedHashSet<>(canonicalize(roles));
    }

    /**
     * Constructs a new user with a set of roles and no full name.
     *
     * @param email        unique email address
     * @param passwordHash BCrypt hash of the password
     * @param roles        non-empty set of roles; each element is canonicalised
     * @param active       whether the account is active
     */
    public User(final String email, final String passwordHash,
                final Set<UserRole> roles, final boolean active) {
        this(email, passwordHash, null, roles, active);
    }

    /**
     * Constructs a new user with a single role.
     *
     * @param email        unique email address
     * @param passwordHash BCrypt hash of the password
     * @param role         user role
     * @param active       whether the account is active
     * @deprecated Use {@link #User(String, String, Set, boolean)} to support multiple roles.
     */
    @Deprecated
    public User(final String email, final String passwordHash,
                final UserRole role, final boolean active) {
        this(email, passwordHash, null, new LinkedHashSet<>(Set.of(role.canonical())), active);
    }

    /**
     * Constructs a new user with a full name and a single role.
     *
     * @param email        unique email address
     * @param passwordHash BCrypt hash of the password
     * @param fullName     display name
     * @param role         user role
     * @param active       whether the account is active
     * @deprecated Use {@link #User(String, String, String, Set, boolean)} to support multiple roles.
     */
    @Deprecated
    public User(final String email, final String passwordHash, final String fullName,
                final UserRole role, final boolean active) {
        this(email, passwordHash, fullName, new LinkedHashSet<>(Set.of(role.canonical())), active);
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
     * Returns all roles assigned to this user.
     *
     * @return unmodifiable view of the roles set
     */
    public Set<UserRole> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Replaces all roles for this user and updates the updatedAt timestamp.
     *
     * @param roles non-empty set of roles
     * @throws IllegalArgumentException if {@code roles} is null or empty
     */
    public void setRoles(final Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        this.roles = new LinkedHashSet<>(roles);
        this.updatedAt = Instant.now();
    }

    /**
     * Adds a role to this user and updates the updatedAt timestamp.
     * The role is canonicalised before being stored so that deprecated aliases
     * never reach the {@code user_roles} junction table.
     *
     * @param role role to add
     */
    public void addRole(final UserRole role) {
        this.roles.add(role.canonical());
        this.updatedAt = Instant.now();
    }

    /**
     * Removes a role from this user and updates the updatedAt timestamp.
     * The role is canonicalised before the lookup/removal so that passing a
     * deprecated alias (e.g. {@code ADMIN}) correctly targets the stored
     * canonical value ({@code SYSTEM_ADMINISTRATOR}).
     *
     * @param role role to remove
     * @throws IllegalStateException if removing the role would leave the user with no roles
     */
    public void removeRole(final UserRole role) {
        final UserRole canonical = role.canonical();
        if (this.roles.size() <= 1 && this.roles.contains(canonical)) {
            throw new IllegalStateException("user must have at least one role");
        }
        this.roles.remove(canonical);
        this.updatedAt = Instant.now();
    }

    /**
     * Returns {@code true} if the given role is among this user's roles.
     *
     * @param role role to test
     * @return true if present
     */
    public boolean hasRole(final UserRole role) {
        return roles.contains(role);
    }

    /**
     * Returns the first role in the set.
     *
     * @return first assigned role
     * @throws IllegalStateException if the roles set is empty
     * @deprecated Use {@link #getRoles()} to support multiple roles.
     */
    @Deprecated
    public UserRole getRole() {
        if (roles.isEmpty()) {
            throw new IllegalStateException("user has no roles");
        }
        return roles.iterator().next();
    }

    /**
     * Replaces all roles with a single role.
     *
     * @param role the sole role to assign
     * @deprecated Use {@link #setRoles(Set)} to support multiple roles.
     */
    @Deprecated
    public void setRole(final UserRole role) {
        setRoles(new LinkedHashSet<>(Set.of(role)));
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

    /**
     * Returns the user's preferred language (locale).
     *
     * @return language code (e.g., 'en', 'uk')
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the user's preferred language and updates the updatedAt timestamp.
     *
     * @param language language code (e.g., 'en', 'uk')
     */
    public void setLanguage(final String language) {
        this.language = language;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the number of consecutive failed login attempts since the counter was last reset.
     *
     * @return failed attempt count
     */
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /**
     * Sets the failed login attempt counter and updates the updatedAt timestamp.
     *
     * @param failedLoginAttempts new value
     */
    public void setFailedLoginAttempts(final int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the instant until which the account is locked, or {@code null} if not locked.
     *
     * @return lock expiry or null
     */
    public Instant getLockedUntil() {
        return lockedUntil;
    }

    /**
     * Sets the instant until which the account is locked and updates the updatedAt timestamp.
     * Pass {@code null} to clear the lockout.
     *
     * @param lockedUntil lock expiry timestamp or null
     */
    public void setLockedUntil(final Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns {@code true} if the account is currently locked (lockedUntil is in the future).
     *
     * @return true if locked
     */
    public boolean isCurrentlyLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    // ---- private helpers --------------------------------------------------

    private static Set<UserRole> canonicalize(final Set<UserRole> roles) {
        Set<UserRole> result = new LinkedHashSet<>();
        for (UserRole r : roles) {
            result.add(r.canonical());
        }
        return result;
    }
}
