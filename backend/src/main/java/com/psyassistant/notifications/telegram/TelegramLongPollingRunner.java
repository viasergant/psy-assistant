package com.psyassistant.notifications.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls updates of the <strong>therapist</strong> bot with {@code getUpdates} in polling mode.
 */
@Component
@ConditionalOnExpression("${notifications.telegram.enabled:false} "
        + "and '${notifications.telegram.mode:polling}'.equalsIgnoreCase('polling')")
public class TelegramLongPollingRunner {

    private final TelegramPoller poller;
    private final TelegramClient client;
    private final TelegramUpdateHandler handler;
    private final TelegramProperties props;

    /**
     * Creates the runner.
     *
     * @param jdbc    JDBC access to the offset row
     * @param client  therapist bot API
     * @param handler therapist bot update handler
     * @param props   configuration
     */
    public TelegramLongPollingRunner(final JdbcTemplate jdbc, final TelegramClient client,
                                     final TelegramUpdateHandler handler, final TelegramProperties props) {
        this.poller = new TelegramPoller(jdbc, props.getPollTimeoutSeconds());
        this.client = client;
        this.handler = handler;
        this.props = props;
    }

    /** Scheduled entry point; one long-poll per invocation. */
    @Scheduled(fixedDelayString = "${notifications.telegram.poll-delay-ms:500}")
    @Transactional
    public void poll() {
        if (props.isOperational()) {
            poller.pollOnce(TelegramPoller.THERAPIST_BOT, client, handler);
        }
    }
}
