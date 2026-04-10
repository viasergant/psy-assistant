package com.psyassistant.notifications.gmail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration properties for the email notification subsystem.
 *
 * <p>Bound from the {@code notifications.email} namespace in {@code application.yml}.
 * SMTP username and password are provided by Spring Mail's own
 * {@code spring.mail.username} / {@code spring.mail.password} properties.
 */
@Component
@ConfigurationProperties(prefix = "notifications.email")
public class GmailEmailProperties {

    /** Envelope-from address shown to recipients. */
    private String senderAddress = "";

    /** Display name for the From header. */
    private String senderName = "PSY Assistant";

    /** HMAC-SHA256 secret used to validate incoming bounce webhook requests. */
    private String webhookSecret = "";

    /** Maximum number of delivery attempts before marking a row as FAILED. */
    private int maxRetries = 5;

    private Outbox outbox = new Outbox();

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(final String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(final String senderName) {
        this.senderName = senderName;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(final String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(final Outbox outbox) {
        this.outbox = outbox;
    }

    /**
     * Outbox poller configuration nested under {@code notifications.email.outbox}.
     */
    public static class Outbox {

        /** Interval in milliseconds between poller runs. */
        private long pollIntervalMs = 5000L;

        /** Maximum number of rows processed per poller invocation. */
        private int batchSize = 10;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(final long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(final int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
