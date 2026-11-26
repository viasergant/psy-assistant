package com.psyassistant.notifications.telegram;

/**
 * Receives the updates of one bot. Shared by the webhook and polling transports.
 */
public interface TelegramUpdateSink {

    /**
     * Handles a text message.
     *
     * @param update parsed update
     */
    void handle(TelegramUpdate update);

    /**
     * Handles an inline-button press.
     *
     * @param callback parsed button press
     */
    void handleCallback(TelegramCallback callback);
}
