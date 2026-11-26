package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Validated view of an inline-button press ({@code callback_query}).
 *
 * @param callbackId Telegram callback query id, needed to acknowledge the press
 * @param chatId     chat the button was pressed in (never logged)
 * @param chatType   Telegram chat type, for example {@code private}
 * @param messageId  message the button belongs to
 * @param data       callback payload set by this application
 */
public record TelegramCallback(String callbackId, String chatId, String chatType, long messageId, String data) {

    /**
     * Parses a raw update; returns empty for anything that is not a button press on a message.
     *
     * @param node raw update
     * @return parsed callback, or empty
     */
    public static Optional<TelegramCallback> parse(final JsonNode node) {
        if (node == null) {
            return Optional.empty();
        }
        final JsonNode cb = node.path("callback_query");
        final JsonNode message = cb.path("message");
        final JsonNode chat = message.path("chat");
        if (!cb.hasNonNull("id") || !cb.hasNonNull("data") || !chat.hasNonNull("id")
                || !message.hasNonNull("message_id")) {
            return Optional.empty();
        }
        return Optional.of(new TelegramCallback(cb.get("id").asText(), chat.get("id").asText(),
                chat.path("type").asText(""), message.get("message_id").asLong(), cb.get("data").asText()));
    }
}
