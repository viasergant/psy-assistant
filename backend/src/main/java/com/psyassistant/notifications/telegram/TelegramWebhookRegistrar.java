package com.psyassistant.notifications.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Aligns Telegram's webhook registration of both bots with the configured mode on startup. Failures are logged
 * and never prevent the application from starting.
 */
@Component
public class TelegramWebhookRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramWebhookRegistrar.class);

    private final TelegramProperties props;
    private final TelegramClient therapistBot;
    private final TelegramClient clientBot;

    /**
     * Creates the registrar.
     *
     * @param props        configuration
     * @param therapistBot therapist bot API
     * @param clientBot    client bot API
     */
    public TelegramWebhookRegistrar(final TelegramProperties props, final TelegramClient therapistBot,
                                    @Qualifier(TelegramConfig.CLIENT_BOT) final TelegramClient clientBot) {
        this.props = props;
        this.therapistBot = therapistBot;
        this.clientBot = clientBot;
    }

    /** Registers or removes the webhooks once the application is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void align() {
        if (props.isOperational()) {
            align("therapist", therapistBot, props.getWebhookUrl(), props.getWebhookSecret());
        }
        if (props.isClientBotOperational()) {
            align("client", clientBot, props.getClientBot().getWebhookUrl(),
                    props.getClientBot().getWebhookSecret());
        }
    }

    private void align(final String name, final TelegramClient client, final String url, final String secret) {
        try {
            if (props.getMode() == TelegramProperties.Mode.WEBHOOK) {
                if (url.isBlank() || secret.isBlank()) {
                    LOG.error("Telegram {} bot in webhook mode needs a webhook url and secret; not registering",
                            name);
                    return;
                }
                client.setWebhook(url, secret);
            } else {
                client.deleteWebhook();
            }
            LOG.info("Telegram {} bot configured in {} mode", name, props.getMode());
        } catch (TelegramApiException e) {
            LOG.error("Telegram {} bot webhook alignment failed: {}", name, e.getMessage());
        }
    }
}
