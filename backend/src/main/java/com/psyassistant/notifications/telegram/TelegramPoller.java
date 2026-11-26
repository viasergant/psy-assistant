package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * One long-poll round for one bot. Each bot has its own offset row in {@code telegram_poll_state}
 * (global infrastructure, no tenant data, hence no {@code org_id}). Locking the row with
 * {@code SKIP LOCKED} makes exactly one instance poll a given bot at a time. Must run inside a transaction.
 */
final class TelegramPoller {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramPoller.class);

    /** Offset row of the therapist bot. */
    static final int THERAPIST_BOT = 1;
    /** Offset row of the client bot. */
    static final int CLIENT_BOT = 2;

    private final JdbcTemplate jdbc;
    private final int timeoutSeconds;

    TelegramPoller(final JdbcTemplate jdbc, final int timeoutSeconds) {
        this.jdbc = jdbc;
        this.timeoutSeconds = timeoutSeconds;
    }

    void pollOnce(final int stateId, final TelegramClient client, final TelegramUpdateSink sink) {
        final List<Long> locked = jdbc.queryForList(
                "SELECT last_update FROM telegram_poll_state WHERE id = ? FOR UPDATE SKIP LOCKED", Long.class,
                stateId);
        if (locked.isEmpty()) {
            return; // another instance is polling this bot
        }
        long offset = locked.get(0);
        final List<JsonNode> updates;
        try {
            updates = client.getUpdates(offset + 1, timeoutSeconds);
        } catch (TelegramApiException e) {
            LOG.warn("Telegram polling failed (bot {}): {}", stateId, e.getMessage());
            return;
        }
        for (final JsonNode raw : updates) {
            offset = Math.max(offset, raw.path("update_id").asLong(offset));
            try {
                TelegramUpdate.parse(raw).ifPresent(sink::handle);
                TelegramCallback.parse(raw).ifPresent(sink::handleCallback);
            } catch (RuntimeException e) {
                LOG.warn("Telegram update handling failed: {}", e.getClass().getSimpleName());
            }
        }
        if (!updates.isEmpty()) {
            jdbc.update("UPDATE telegram_poll_state SET last_update = ? WHERE id = ?", offset, stateId);
        }
    }
}
