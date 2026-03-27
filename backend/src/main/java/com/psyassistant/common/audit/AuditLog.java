package com.psyassistant.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of a security-relevant event (login attempt, logout, token refresh).
 *
 * <p>Rows are append-only — they are never updated or deleted.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_attempted", length = 255)
    private String emailAttempted;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(length = 20)
    private String outcome;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected AuditLog() {
    }

    /**
     * Private constructor — use {@link Builder}.
     */
    private AuditLog(final Builder builder) {
        this.eventType = builder.eventType;
        this.userId = builder.userId;
        this.emailAttempted = builder.emailAttempted;
        this.ipAddress = builder.ipAddress;
        this.requestId = builder.requestId;
        this.outcome = builder.outcome;
        this.detail = builder.detail;
        this.createdAt = Instant.now();
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
     * Returns the event type (e.g. LOGIN_SUCCESS).
     *
     * @return event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Returns the user ID associated with the event.
     *
     * @return user UUID or null for unauthenticated events
     */
    public UUID getUserId() {
        return userId;
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
     * Builder for {@link AuditLog}.
     */
    public static final class Builder {

        private final String eventType;
        private UUID userId;
        private String emailAttempted;
        private String ipAddress;
        private String requestId;
        private String outcome;
        private String detail;

        /**
         * Creates a builder with the mandatory event type.
         *
         * @param eventType event type string (e.g. LOGIN_SUCCESS)
         */
        public Builder(final String eventType) {
            this.eventType = eventType;
        }

        /**
         * Sets the user ID.
         *
         * @param value user UUID
         * @return this builder
         */
        public Builder userId(final UUID value) {
            this.userId = value;
            return this;
        }

        /**
         * Sets the email address that was used in the attempt.
         *
         * @param value email or null
         * @return this builder
         */
        public Builder emailAttempted(final String value) {
            this.emailAttempted = value;
            return this;
        }

        /**
         * Sets the IP address of the caller.
         *
         * @param value IPv4 or IPv6 address string
         * @return this builder
         */
        public Builder ipAddress(final String value) {
            this.ipAddress = value;
            return this;
        }

        /**
         * Sets the correlation request ID.
         *
         * @param value request ID string
         * @return this builder
         */
        public Builder requestId(final String value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the outcome (SUCCESS or FAILURE).
         *
         * @param value outcome string
         * @return this builder
         */
        public Builder outcome(final String value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets an optional free-text detail.
         *
         * @param value detail string (max 500 chars)
         * @return this builder
         */
        public Builder detail(final String value) {
            this.detail = value;
            return this;
        }

        /**
         * Builds and returns a new {@link AuditLog} instance.
         *
         * @return new audit log record
         */
        public AuditLog build() {
            return new AuditLog(this);
        }
    }
}
