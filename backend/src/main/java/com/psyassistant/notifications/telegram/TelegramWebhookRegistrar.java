package com.psyassistant.notifications.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Aligns Telegram's webhook registration with the configured mode on startup. Failures are logged
 * and never prevent the application from starting.
 */
@Component
public class TelegramWebhookRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramWebhookRegistrar.class);

    private final TelegramProperties props;
    private final TelegramClient client;

    /**
     * Creates the registrar.
     *
     * @param props  configuration
     * @param client Telegram API
     */
    public TelegramWebhookRegistrar(final TelegramProperties props, final TelegramClient client) {
        this.props = props;
        this.client = client;
    }

    /** Registers or removes the webhook once the application is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void align() {
        if (!props.isOperational()) {
            return;
        }
        try {
            if (props.getMode() == TelegramProperties.Mode.WEBHOOK) {
                if (props.getWebhookUrl().isBlank() || props.getWebhookSecret().isBlank()) {
                    LOG.error("Telegram webhook mode needs webhook-url and webhook-secret; not registering");
                    return;
                }
                client.setWebhook(props.getWebhookUrl(), props.getWebhookSecret());
            } else {
                client.deleteWebhook();
            }
            LOG.info("Telegram configured in {} mode", props.getMode());
        } catch (TelegramApiException e) {
            LOG.error("Telegram webhook alignment failed: {}", e.getMessage());
        }
    }
}
