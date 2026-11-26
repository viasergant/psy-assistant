package com.psyassistant.notifications.telegram;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * One-time token binding a Telegram chat to a user. Only the SHA-256 hash is stored.
 */
@Filter(name = "orgFilter", condition = "org_id = :orgId")
@Entity
@Table(name = "telegram_link_token")
public class TelegramLinkToken extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "language", nullable = false, length = 5)
    private String language = "en";

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected TelegramLinkToken() {
    }

    /**
     * Creates a token record.
     *
     * @param orgId     owning organization
     * @param userId    user the chat will be bound to
     * @param tokenHash SHA-256 hex of the raw token
     * @param expiresAt expiry instant
     */
    public TelegramLinkToken(final UUID orgId, final UUID userId, final String tokenHash,
                             final Instant expiresAt) {
        this.orgId = orgId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /**
     * Creates a token record that binds a chat to a client.
     *
     * @param orgId     owning organization
     * @param clientId  client the chat will be bound to
     * @param language  en or uk, used for bot replies
     * @param timezone  IANA zone used to render session times for the client
     * @param tokenHash SHA-256 hex of the raw token
     * @param expiresAt expiry instant
     * @return the token
     */
    public static TelegramLinkToken forClient(final UUID orgId, final UUID clientId, final String language,
                                              final String timezone, final String tokenHash,
                                              final Instant expiresAt) {
        final TelegramLinkToken token = new TelegramLinkToken(orgId, null, tokenHash, expiresAt);
        token.clientId = clientId;
        token.language = language;
        token.timezone = timezone;
        return token;
    }

    /**
     * Returns whether the token can still be redeemed.
     *
     * @param now current time
     * @return true when unused and unexpired
     */
    public boolean isRedeemable(final Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(final Instant now) {
        this.usedAt = now;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getLanguage() {
        return language;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
