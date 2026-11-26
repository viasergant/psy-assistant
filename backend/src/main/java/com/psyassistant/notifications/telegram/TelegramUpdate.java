package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Minimal, validated view of an incoming Telegram update.
 *
 * @param updateId Telegram update id
 * @param chatId   chat id as string (never logged)
 * @param chatType Telegram chat type, for example {@code private}
 * @param text     message text, may be null
 */
public record TelegramUpdate(long updateId, String chatId, String chatType, String text) {

    /**
     * Parses a raw Telegram update JSON node; returns empty for anything that is not a text message.
     *
     * @param node raw update
     * @return parsed update, or empty
     */
    public static Optional<TelegramUpdate> parse(final JsonNode node) {
        if (node == null || !node.hasNonNull("update_id")) {
            return Optional.empty();
        }
        final JsonNode message = node.path("message");
        final JsonNode chat = message.path("chat");
        if (!chat.hasNonNull("id") || !message.hasNonNull("text")) {
            return Optional.empty();
        }
        return Optional.of(new TelegramUpdate(
                node.get("update_id").asLong(),
                chat.get("id").asText(),
                chat.path("type").asText(""),
                message.get("text").asText()));
    }
}
