package com.psyassistant.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of a password reset token.
 *
 * <p>Only the SHA-256 hex digest of the raw token is stored — the raw value is sent
 * in the email link but never persisted.  A token is single-use: {@code usedAt} is
 * set on first redemption and the token is rejected thereafter.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex of the raw token sent in the reset email. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set when the token has been consumed. Non-null means single-use exhausted. */
    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected PasswordResetToken() {
    }

    /**
     * Constructs a new, unused password reset token record.
     *
     * @param user      the account for which the reset was requested
     * @param tokenHash SHA-256 hex of the raw token
     * @param expiresAt when the token expires (24 h from creation)
     */
    public PasswordResetToken(final User user, final String tokenHash, final Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    /** Returns the record's primary key. */
    public UUID getId() {
        return id;
    }

    /** Returns the owning user. */
    public User getUser() {
        return user;
    }

    /** Returns the SHA-256 hex of the raw token. */
    public String getTokenHash() {
        return tokenHash;
    }

    /** Returns when this token expires. */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** Returns when this token was consumed, or {@code null} if not yet used. */
    public Instant getUsedAt() {
        return usedAt;
    }

    /** Returns when this record was created. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns true if the token has already been redeemed.
     *
     * @return true if usedAt is set
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Returns true if the token is within its validity window and has not been redeemed.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return !isUsed() && Instant.now().isBefore(expiresAt);
    }

    /**
     * Marks this token as consumed by recording the redemption timestamp.
     */
    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
