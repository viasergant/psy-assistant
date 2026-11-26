package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * Receives updates of the therapist bot in webhook mode. The endpoint is public, so every request must carry the
 * configured secret in {@code X-Telegram-Bot-Api-Secret-Token}; a blank secret rejects everything.
 */
@RestController
@RequestMapping("/api/v1/telegram/webhook")
@ConditionalOnExpression("${notifications.telegram.enabled:false} "
        + "and '${notifications.telegram.mode:polling}'.equalsIgnoreCase('webhook')")
public class TelegramWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramProperties props;
    private final TelegramUpdateHandler handler;

    /**
     * Creates the controller.
     *
     * @param props   configuration
     * @param handler update handler
     */
    public TelegramWebhookController(final TelegramProperties props, final TelegramUpdateHandler handler) {
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
        if (!secretMatches(secret)) {
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

    private boolean secretMatches(final String provided) {
        return secretMatches(props.getWebhookSecret(), provided);
    }

    /**
     * Constant-time comparison of the configured and provided secret; a blank configured secret never matches.
     *
     * @param expected configured secret
     * @param provided value of the secret-token header
     * @return true when they match
     */
    static boolean secretMatches(final String expected, final String provided) {
        if (expected == null || expected.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
