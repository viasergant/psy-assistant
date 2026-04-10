package com.psyassistant.notifications.webhook;

import com.psyassistant.notifications.gmail.GmailEmailProperties;
import com.psyassistant.notifications.log.EmailDeliveryLog;
import com.psyassistant.notifications.log.EmailDeliveryLogRepository;
import com.psyassistant.notifications.log.EmailDeliveryStatus;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that receives Gmail bounce event webhooks.
 *
 * <p>The incoming request is validated with HMAC-SHA256 using
 * {@code notifications.email.webhook-secret} before any state is modified.
 * An invalid or missing signature always returns HTTP 401.
 *
 * <p>This endpoint is intentionally open to unauthenticated callers
 * (the HMAC signature is the authentication mechanism).
 */
@RestController
@RequestMapping("/api/v1/email/webhooks")
public class EmailWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(EmailWebhookController.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final EmailDeliveryLogRepository repository;
    private final GmailEmailProperties properties;

    /**
     * Constructs the controller.
     *
     * @param repository outbox delivery log repository
     * @param properties email configuration (webhook-secret)
     */
    public EmailWebhookController(
            final EmailDeliveryLogRepository repository,
            final GmailEmailProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Accepts a Gmail bounce notification and marks the corresponding row as
     * {@link EmailDeliveryStatus#BOUNCED}.
     *
     * <p>The operation is idempotent: replaying the same webhook for an already-bounced
     * message is a no-op.
     *
     * @param signature the HMAC-SHA256 hex digest of the body, provided in
     *                  {@code X-Webhook-Secret} header
     * @param body      the raw JSON payload
     * @return 200 on success, 401 on invalid signature, 404 if messageId unknown
     */
    @PostMapping("/gmail")
    @Transactional
    public ResponseEntity<Void> handleBounce(
            @RequestHeader(value = "X-Webhook-Secret", required = false) final String signature,
            @RequestBody final String body) {

        if (!isValidSignature(body, signature)) {
            LOG.warn("EMAIL_WEBHOOK_INVALID_SIGNATURE received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String messageId = extractMessageId(body);
        if (messageId == null) {
            LOG.warn("EMAIL_WEBHOOK_MISSING_MESSAGE_ID");
            return ResponseEntity.badRequest().build();
        }

        final Optional<EmailDeliveryLog> optLog = repository.findByProviderMessageId(messageId);
        if (optLog.isEmpty()) {
            LOG.warn("EMAIL_WEBHOOK_UNKNOWN_MESSAGE_ID messageId={}", messageId);
            return ResponseEntity.notFound().build();
        }

        final EmailDeliveryLog entry = optLog.get();
        if (entry.getStatus() != EmailDeliveryStatus.BOUNCED) {
            entry.setStatus(EmailDeliveryStatus.BOUNCED);
            repository.save(entry);
            LOG.info("EMAIL_BOUNCED logId={} recipientAddressHash={}",
                    entry.getId(), entry.getRecipientAddressHash());
        }

        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(final String body, final String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        final String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            LOG.error("EMAIL_WEBHOOK_SECRET_NOT_CONFIGURED — all webhook requests will be rejected");
            return false;
        }
        try {
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            final byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            final byte[] actual = HexFormat.of().parseHex(providedSignature);
            return MessageDigest.isEqual(expected, actual);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException ex) {
            LOG.warn("EMAIL_WEBHOOK_SIGNATURE_CHECK_ERROR error={}", ex.getMessage());
            return false;
        }
    }

    /**
     * Minimal JSON field extraction without pulling in a full JSON library dependency.
     * Parses {@code "messageId"} from the webhook payload using string search.
     */
    private static String extractMessageId(final String json) {
        final String key = "\"messageId\"";
        final int keyIdx = json.indexOf(key);
        if (keyIdx < 0) {
            return null;
        }
        final int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) {
            return null;
        }
        final int openQuote = json.indexOf('"', colonIdx + 1);
        if (openQuote < 0) {
            return null;
        }
        final int closeQuote = json.indexOf('"', openQuote + 1);
        if (closeQuote < 0) {
            return null;
        }
        return json.substring(openQuote + 1, closeQuote);
    }
}
