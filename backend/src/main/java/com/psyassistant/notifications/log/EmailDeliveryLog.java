package com.psyassistant.notifications.log;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.common.config.EncryptedStringConverter;
import com.psyassistant.notifications.NotificationEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity that records every outbound email attempt (transactional outbox pattern).
 *
 * <p>The recipient address is stored encrypted at rest via
 * {@link EncryptedStringConverter} (AES-256-GCM). A non-reversible SHA-256 hex hash is
 * stored alongside for bounce webhook lookups and log correlation.
 * <strong>Never log {@code recipientAddressEncrypted}; use {@code recipientAddressHash}.</strong>
 */
@Entity
@Table(name = "email_delivery_log")
public class EmailDeliveryLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private NotificationEventType eventType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "recipient_address_encrypted", nullable = false)
    private String recipientAddressEncrypted;

    @Column(name = "recipient_address_hash", nullable = false, length = 64)
    private String recipientAddressHash;

    @Column(name = "subject_template_key", nullable = false)
    private String subjectTemplateKey;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmailDeliveryStatus status;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Default constructor required by JPA.
     */
    protected EmailDeliveryLog() {
    }

    /**
     * Creates a new log entry in {@link EmailDeliveryStatus#PENDING} state.
     *
     * @param eventType               the notification category
     * @param recipientAddressEncrypted encrypted recipient address (via converter)
     * @param recipientAddressHash    SHA-256 hex of the plaintext address
     * @param subjectTemplateKey      i18n key for the subject line
     * @param body                    pre-rendered email body text
     */
    public EmailDeliveryLog(
            final NotificationEventType eventType,
            final String recipientAddressEncrypted,
            final String recipientAddressHash,
            final String subjectTemplateKey,
            final String body) {
        this.eventType = eventType;
        this.recipientAddressEncrypted = recipientAddressEncrypted;
        this.recipientAddressHash = recipientAddressHash;
        this.subjectTemplateKey = subjectTemplateKey;
        this.body = body;
        this.status = EmailDeliveryStatus.PENDING;
        this.retryCount = 0;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public String getRecipientAddressEncrypted() {
        return recipientAddressEncrypted;
    }

    public String getRecipientAddressHash() {
        return recipientAddressHash;
    }

    public String getSubjectTemplateKey() {
        return subjectTemplateKey;
    }

    public String getBody() {
        return body;
    }

    public EmailDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(final EmailDeliveryStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(final String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(final int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(final Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(final Instant sentAt) {
        this.sentAt = sentAt;
    }
}
