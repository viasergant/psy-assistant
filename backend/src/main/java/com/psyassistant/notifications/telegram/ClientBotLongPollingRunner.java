package com.psyassistant.notifications.telegram;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls updates of the <strong>client</strong> bot with {@code getUpdates} in polling mode. Idle (returns at once)
 * when the client bot has no token.
 */
@Component
@ConditionalOnExpression("${notifications.telegram.enabled:false} "
        + "and '${notifications.telegram.mode:polling}'.equalsIgnoreCase('polling')")
public class ClientBotLongPollingRunner {

    private final TelegramPoller poller;
    private final TelegramClient client;
    private final ClientBotUpdateHandler handler;
    private final TelegramProperties props;

    /**
     * Creates the runner.
     *
     * @param jdbc    JDBC access to the offset row
     * @param client  client bot API
     * @param handler client bot update handler
     * @param props   configuration
     */
    public ClientBotLongPollingRunner(final JdbcTemplate jdbc,
                                      @Qualifier(TelegramConfig.CLIENT_BOT) final TelegramClient client,
                                      final ClientBotUpdateHandler handler, final TelegramProperties props) {
        this.poller = new TelegramPoller(jdbc, props.getPollTimeoutSeconds());
        this.client = client;
        this.handler = handler;
        this.props = props;
    }

    /** Scheduled entry point; one long-poll per invocation. */
    @Scheduled(fixedDelayString = "${notifications.telegram.poll-delay-ms:500}")
    @Transactional
    public void poll() {
        if (props.isClientBotOperational()) {
            poller.pollOnce(TelegramPoller.CLIENT_BOT, client, handler);
        }
    }
}
