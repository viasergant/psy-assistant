package com.psyassistant.notifications.telegram;

/**
 * Failure calling the Telegram Bot API. The message never contains the bot token.
 */
public class TelegramApiException extends RuntimeException {

    private final int statusCode;
    private final int retryAfterSeconds;

    /**
     * Creates the exception.
     *
     * @param message           safe description
     * @param statusCode        HTTP status, or 0 for transport errors
     * @param retryAfterSeconds Telegram {@code retry_after} for HTTP 429, else 0
     */
    public TelegramApiException(final String message, final int statusCode, final int retryAfterSeconds) {
        super(message);
        this.statusCode = statusCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Returns whether the user blocked the bot or the chat no longer exists.
     *
     * @return true when the recipient is permanently unreachable
     */
    public boolean isRecipientGone() {
        return statusCode == 403 || statusCode == 400 && getMessage().contains("chat not found");
    }

    /**
     * Returns whether retrying can help.
     *
     * @return true for transport errors, 429 and 5xx
     */
    public boolean isRetryable() {
        return statusCode == 0 || statusCode == 429 || statusCode >= 500;
    }
}
