package com.psyassistant.notifications.telegram;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Outbox row for one Telegram message. {@code dedupKey} is unique, which makes scheduling
 * idempotent across restarts and multiple instances.
 *
 * <p>The recipient is always resolved from {@link TherapistNotificationSettings} by
 * {@code userId} at send time; no chat id is stored here. <strong>Never log {@code body}.</strong>
 */
@Filter(name = "orgFilter", condition = "org_id = :orgId")
@Entity
@Table(name = "telegram_delivery_log")
public class TelegramDeliveryLog extends BaseEntity {

    /** Message category. */
    public enum Kind { DIGEST, REMINDER, CLIENT_REMINDER, SYSTEM }

    /** Delivery state. */
    public enum Status { PENDING, RETRY, SENT, FAILED, CANCELLED }

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "client_id")
    private UUID clientId;

    /** Appointment a client message is about; drives the cancel button. */
    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(name = "dedup_key", nullable = false, length = 255, updatable = false)
    private String dedupKey;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error", length = 255)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected TelegramDeliveryLog() {
    }

    /**
     * Creates a pending delivery.
     *
     * @param orgId    owning organization
     * @param userId   recipient user
     * @param kind     message category
     * @param dedupKey idempotency key
     * @param body     rendered text
     */
    public TelegramDeliveryLog(final UUID orgId, final UUID userId, final Kind kind,
                               final String dedupKey, final String body) {
        this.orgId = orgId;
        this.userId = userId;
        this.kind = kind;
        this.dedupKey = dedupKey;
        this.body = body;
    }

    /**
     * Creates a pending delivery addressed to a client.
     *
     * @param orgId         owning organization
     * @param clientId      recipient client
     * @param appointmentId appointment the message is about, or null
     * @param kind          message category
     * @param dedupKey      idempotency key
     * @param body          rendered text
     * @return the delivery
     */
    public static TelegramDeliveryLog forClient(final UUID orgId, final UUID clientId, final UUID appointmentId,
                                                final Kind kind, final String dedupKey, final String body) {
        final TelegramDeliveryLog log = new TelegramDeliveryLog(orgId, null, kind, dedupKey, body);
        log.clientId = clientId;
        log.appointmentId = appointmentId;
        return log;
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

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public Kind getKind() {
        return kind;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public String getBody() {
        return body;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
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
