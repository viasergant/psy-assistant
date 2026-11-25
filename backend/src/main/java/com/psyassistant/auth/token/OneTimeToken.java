package com.psyassistant.auth.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Single-use, expiring token. Only the SHA-256 hash of the raw token is persisted.
 */
@Entity
@Table(name = "one_time_tokens")
public class OneTimeToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected OneTimeToken() {
    }

    /**
     * Creates a new unconsumed token.
     *
     * @param userId    owner of the token
     * @param purpose   what the token may be used for
     * @param tokenHash SHA-256 hex digest of the raw token
     * @param expiresAt instant after which the token is invalid
     */
    public OneTimeToken(final UUID userId, final TokenPurpose purpose,
                        final String tokenHash, final Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    /**
     * Returns the token owner's user id.
     *
     * @return user id
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Returns the purpose this token was issued for.
     *
     * @return purpose
     */
    public TokenPurpose getPurpose() {
        return purpose;
    }

    /**
     * Returns whether the token has already been used.
     *
     * @return true if consumed
     */
    public boolean isConsumed() {
        return consumedAt != null;
    }

    /**
     * Returns whether the token is expired at the given instant.
     *
     * @param now the instant to compare against
     * @return true if expired
     */
    public boolean isExpired(final Instant now) {
        return !expiresAt.isAfter(now);
    }

    /**
     * Marks the token as used.
     *
     * @param now consumption time
     */
    public void consume(final Instant now) {
        this.consumedAt = now;
    }
}
