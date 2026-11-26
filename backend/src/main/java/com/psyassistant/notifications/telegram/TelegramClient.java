package com.psyassistant.notifications.telegram;

import java.util.List;

/**
 * Port to the Telegram Bot API.
 */
public interface TelegramClient {

    /**
     * Sends a plain-text message (no parse mode, so content is never interpreted as markup).
     *
     * @param chatId target chat
     * @param text   message text
     * @throws TelegramApiException on any API or transport failure
     */
    void sendMessage(String chatId, String text);

    /**
     * Fetches updates with long polling.
     *
     * @param offset         first update id to return
     * @param timeoutSeconds long-poll timeout
     * @return raw updates
     */
    List<com.fasterxml.jackson.databind.JsonNode> getUpdates(long offset, int timeoutSeconds);

    /**
     * Registers the webhook with a secret token.
     *
     * @param url    public HTTPS endpoint
     * @param secret value Telegram echoes in {@code X-Telegram-Bot-Api-Secret-Token}
     */
    void setWebhook(String url, String secret);

    /** Removes the webhook so that polling works. */
    void deleteWebhook();
}
