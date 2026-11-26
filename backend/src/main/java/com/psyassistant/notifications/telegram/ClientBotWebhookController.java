package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives updates of the <strong>client</strong> bot in webhook mode. The endpoint is public, so every request
 * must carry the client bot's own secret token, which differs from the therapist bot's.
 */
@RestController
@RequestMapping("/api/v1/telegram/client-webhook")
@ConditionalOnExpression("${notifications.telegram.enabled:false} "
        + "and '${notifications.telegram.mode:polling}'.equalsIgnoreCase('webhook')")
public class ClientBotWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(ClientBotWebhookController.class);

    private final TelegramProperties props;
    private final ClientBotUpdateHandler handler;

    /**
     * Creates the controller.
     *
     * @param props   configuration
     * @param handler client bot update handler
     */
    public ClientBotWebhookController(final TelegramProperties props, final ClientBotUpdateHandler handler) {
        this.props = props;
        this.handler = handler;
    }

    /**
     * Accepts one update.
     *
     * @param secret value of the secret-token header
     * @param body   raw update
     * @return 200 when accepted, 403 when the secret does not match
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) final String secret,
            @RequestBody final JsonNode body) {
        if (!TelegramWebhookController.secretMatches(props.getClientBot().getWebhookSecret(), secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            TelegramUpdate.parse(body).ifPresent(handler::handle);
            TelegramCallback.parse(body).ifPresent(handler::handleCallback);
        } catch (RuntimeException e) {
            // Always acknowledge, otherwise Telegram retries the same update indefinitely.
            LOG.warn("Telegram update handling failed: {}", e.getClass().getSimpleName());
        }
        return ResponseEntity.ok().build();
    }
}
