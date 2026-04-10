package com.psyassistant.notifications.gmail;

/**
 * Thrown when the Gmail SMTP adapter cannot send an email due to missing or
 * incomplete provider configuration (e.g., blank {@code EMAIL_SMTP_PASSWORD}).
 *
 * <p>The delivery row is persisted with {@code CONFIG_ERROR} status before this exception
 * is thrown so the failure is traceable without plaintext addresses in logs.
 */
public class EmailConfigException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message.
     *
     * @param message detail explaining which configuration is missing
     */
    public EmailConfigException(final String message) {
        super(message);
    }
}
