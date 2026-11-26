package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls updates with {@code getUpdates} in polling mode.
 *
 * <p>The single-row {@code telegram_poll_state} table is global infrastructure (no tenant data, hence
 * no {@code org_id}). Locking it with {@code SKIP LOCKED} makes exactly one instance poll at a time and
 * keeps the offset consistent.
 */
@Component
@ConditionalOnExpression("${notifications.telegram.enabled:false} "
        + "and '${notifications.telegram.mode:polling}'.equalsIgnoreCase('polling')")
public class TelegramLongPollingRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramLongPollingRunner.class);

    private final JdbcTemplate jdbc;
    private final TelegramClient client;
    private final TelegramUpdateHandler handler;
    private final TelegramProperties props;

    /**
     * Creates the runner.
     *
     * @param jdbc    JDBC access to the offset row
     * @param client  Telegram API
     * @param handler update handler
     * @param props   configuration
     */
    public TelegramLongPollingRunner(final JdbcTemplate jdbc, final TelegramClient client,
                                     final TelegramUpdateHandler handler, final TelegramProperties props) {
        this.jdbc = jdbc;
        this.client = client;
        this.handler = handler;
        this.props = props;
    }

    /** Scheduled entry point; one long-poll per invocation. */
    @Scheduled(fixedDelayString = "${notifications.telegram.poll-delay-ms:500}")
    @Transactional
    public void poll() {
        if (!props.isOperational()) {
            return;
        }
        final List<Long> locked = jdbc.queryForList(
                "SELECT last_update FROM telegram_poll_state WHERE id = 1 FOR UPDATE SKIP LOCKED", Long.class);
        if (locked.isEmpty()) {
            return; // another instance is polling
        }
        long offset = locked.get(0);
        final List<JsonNode> updates;
        try {
            updates = client.getUpdates(offset + 1, props.getPollTimeoutSeconds());
        } catch (TelegramApiException e) {
            LOG.warn("Telegram polling failed: {}", e.getMessage());
            return;
        }
        for (final JsonNode raw : updates) {
            offset = Math.max(offset, raw.path("update_id").asLong(offset));
            try {
                TelegramUpdate.parse(raw).ifPresent(handler::handle);
                TelegramCallback.parse(raw).ifPresent(handler::handleCallback);
            } catch (RuntimeException e) {
                LOG.warn("Telegram update handling failed: {}", e.getClass().getSimpleName());
            }
        }
        if (!updates.isEmpty()) {
            jdbc.update("UPDATE telegram_poll_state SET last_update = ? WHERE id = 1", offset);
        }
    }
}
