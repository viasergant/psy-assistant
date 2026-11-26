package com.psyassistant.notifications.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration for the Telegram notification subsystem ({@code notifications.telegram}).
 *
 * <p>The bot token and webhook secret are secrets: supply them via environment variables or
 * {@code application-secrets.properties}, never commit them, and never log them.
 */
@Component
@ConfigurationProperties(prefix = "notifications.telegram")
public class TelegramProperties {

    /** How the backend receives Telegram updates. */
    public enum Mode {
        /** Telegram pushes updates to {@code /api/v1/telegram/webhook}. */
        WEBHOOK,
        /** The backend pulls updates with {@code getUpdates}. */
        POLLING
    }

    /**
     * Credentials of the separate bot that talks to clients. It is a different bot from the therapist bot,
     * so a client can never reach therapist functions and a leaked token exposes only one audience.
     */
    public static class ClientBot {
        private String botToken = "";
        private String botUsername = "";
        private String webhookSecret = "";
        private String webhookUrl = "";

        public String getBotToken() {
            return botToken;
        }

        public void setBotToken(final String botToken) {
            this.botToken = botToken;
        }

        public String getBotUsername() {
            return botUsername;
        }

        public void setBotUsername(final String botUsername) {
            this.botUsername = botUsername;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(final String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(final String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }

    private boolean enabled;
    private final ClientBot clientBot = new ClientBot();
    private String botToken = "";
    private String botUsername = "";
    private Mode mode = Mode.POLLING;
    private String webhookSecret = "";
    private String webhookUrl = "";
    private int maxRetries = 5;
    private long outboxPollMs = 5000L;
    private int outboxBatchSize = 20;
    private int pollTimeoutSeconds = 25;
    private long linkTokenTtlMinutes = 10L;
    private int clientReminderMinutes = 1440;
    private int lateCancellationHours = 24;

    /**
     * Returns whether Telegram can actually operate: switched on and a bot token is present.
     *
     * @return true when sending and linking are possible
     */
    public boolean isOperational() {
        return enabled && botToken != null && !botToken.isBlank();
    }

    /**
     * Returns whether the client bot can operate: Telegram is switched on and the client bot has a token.
     *
     * @return true when client invites, reminders and cancellations are possible
     */
    public boolean isClientBotOperational() {
        return enabled && clientBot.getBotToken() != null && !clientBot.getBotToken().isBlank();
    }

    public ClientBot getClientBot() {
        return clientBot;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(final String botToken) {
        this.botToken = botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(final String botUsername) {
        this.botUsername = botUsername;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(final String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(final String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getOutboxPollMs() {
        return outboxPollMs;
    }

    public void setOutboxPollMs(final long outboxPollMs) {
        this.outboxPollMs = outboxPollMs;
    }

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(final int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public int getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }

    public void setPollTimeoutSeconds(final int pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    public int getClientReminderMinutes() {
        return clientReminderMinutes;
    }

    public void setClientReminderMinutes(final int clientReminderMinutes) {
        this.clientReminderMinutes = clientReminderMinutes;
    }

    public int getLateCancellationHours() {
        return lateCancellationHours;
    }

    public void setLateCancellationHours(final int lateCancellationHours) {
        this.lateCancellationHours = lateCancellationHours;
    }

    public long getLinkTokenTtlMinutes() {
        return linkTokenTtlMinutes;
    }

    public void setLinkTokenTtlMinutes(final long linkTokenTtlMinutes) {
        this.linkTokenTtlMinutes = linkTokenTtlMinutes;
    }
}
