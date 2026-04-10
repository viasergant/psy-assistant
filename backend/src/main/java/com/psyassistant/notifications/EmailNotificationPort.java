package com.psyassistant.notifications;

/**
 * Port for queuing outbound email notifications.
 *
 * <p>Implementations must persist an outbox entry within the caller's active transaction
 * so that the email is delivered at-least-once even if the process restarts.
 *
 * <p>If the provider credentials are missing or invalid at queue time, the implementation
 * must write a {@code CONFIG_ERROR} delivery log row and throw
 * {@link com.psyassistant.notifications.gmail.EmailConfigException}.
 */
public interface EmailNotificationPort {

    /**
     * Persists an outbox entry in the current transaction for at-least-once delivery.
     *
     * <p>If credentials are not configured, writes a CONFIG_ERROR status row and throws
     * {@link com.psyassistant.notifications.gmail.EmailConfigException}.
     *
     * @param message the email to queue; must not be {@code null}
     */
    void queue(EmailMessage message);
}
