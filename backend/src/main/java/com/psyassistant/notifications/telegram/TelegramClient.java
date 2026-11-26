package com.psyassistant.notifications.telegram;

import java.util.List;

/**
 * Port to the Telegram Bot API.
 */
public interface TelegramClient {

    /**
     * Inline keyboard button.
     *
     * @param text         label
     * @param callbackData payload returned when pressed (at most 64 bytes)
     */
    record Button(String text, String callbackData) { }

    /**
     * Sends a plain-text message (no parse mode, so content is never interpreted as markup).
     *
     * @param chatId target chat
     * @param text   message text
     * @throws TelegramApiException on any API or transport failure
     */
    void sendMessage(String chatId, String text);

    /**
     * Sends a message with one row of inline buttons.
     *
     * @param chatId  target chat
     * @param text    message text
     * @param buttons inline buttons shown under the message
     * @throws TelegramApiException on any API or transport failure
     */
    void sendMessage(String chatId, String text, List<Button> buttons);

    /**
     * Replaces the text and buttons of a message the bot sent earlier.
     *
     * @param chatId    chat containing the message
     * @param messageId message to edit
     * @param text      new text
     * @param buttons   new buttons; empty removes the keyboard
     * @throws TelegramApiException on any API or transport failure
     */
    void editMessage(String chatId, long messageId, String text, List<Button> buttons);

    /**
     * Acknowledges a button press so the client's spinner stops.
     *
     * @param callbackQueryId id of the callback query
     * @throws TelegramApiException on any API or transport failure
     */
    void answerCallbackQuery(String callbackQueryId);

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
