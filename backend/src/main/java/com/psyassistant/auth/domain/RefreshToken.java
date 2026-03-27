package com.psyassistant.auth.domain;

import com.psyassistant.users.User;
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
 * Persistent record of a refresh token.
 *
 * <p>Only the SHA-256 hex digest of the raw token UUID is stored — never the
 * plain-text token value itself.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean revoked;

    /** Required by JPA. */
    protected RefreshToken() {
    }

    /**
     * Constructs a new non-revoked refresh token record.
     *
     * @param user      the owning user
     * @param tokenHash SHA-256 hex of the raw token
     * @param expiresAt when the token expires
     */
    public RefreshToken(final User user, final String tokenHash, final Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    /**
     * Returns the record's primary key.
     *
     * @return UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the owning user.
     *
     * @return user
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the SHA-256 hex digest of the raw token.
     *
     * @return token hash
     */
    public String getTokenHash() {
        return tokenHash;
    }

    /**
     * Returns when this token expires.
     *
     * @return expiry instant
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Returns when this record was created.
     *
     * @return creation instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns whether this token has been explicitly revoked.
     *
     * @return true if revoked
     */
    public boolean isRevoked() {
        return revoked;
    }
}
