package com.psyassistant.notifications.telegram;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.common.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Binds one Telegram chat to one client. The chat id is encrypted at rest; {@code chatIdHash}
 * (SHA-256) enforces that a chat belongs to at most one client. <strong>Never log the chat id.</strong>
 */
@Filter(name = "orgFilter", condition = "org_id = :orgId")
@Entity
@Table(name = "client_telegram_link")
public class ClientTelegramLink extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "chat_id_encrypted", nullable = false)
    private String chatId;

    @Column(name = "chat_id_hash", nullable = false, length = 64, unique = true)
    private String chatIdHash;

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected ClientTelegramLink() {
    }

    /**
     * Creates a link.
     *
     * @param orgId      owning organization
     * @param clientId   linked client
     * @param chatId     plaintext chat id
     * @param chatIdHash SHA-256 hex of the chat id
     * @param language   en or uk, used for bot replies
     * @param timezone   IANA zone used to render session times for the client
     * @param linkedAt   link time
     */
    public ClientTelegramLink(final UUID orgId, final UUID clientId, final String chatId,
                              final String chatIdHash, final String language, final String timezone,
                              final Instant linkedAt) {
        this.orgId = orgId;
        this.clientId = clientId;
        this.chatId = chatId;
        this.chatIdHash = chatIdHash;
        this.language = language;
        this.timezone = timezone;
        this.linkedAt = linkedAt;
    }

    /**
     * Re-binds the link to a (possibly different) chat.
     *
     * @param newChatId     plaintext chat id
     * @param newChatIdHash SHA-256 hex of the chat id
     * @param newLanguage   en or uk
     * @param newTimezone   IANA zone
     * @param now           link time
     */
    public void rebind(final String newChatId, final String newChatIdHash, final String newLanguage,
                       final String newTimezone, final Instant now) {
        this.chatId = newChatId;
        this.chatIdHash = newChatIdHash;
        this.language = newLanguage;
        this.timezone = newTimezone;
        this.linkedAt = now;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getChatIdHash() {
        return chatIdHash;
    }

    public String getLanguage() {
        return language;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
