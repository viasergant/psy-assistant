package com.psyassistant.notifications.gmail;

import com.psyassistant.notifications.EmailMessage;
import com.psyassistant.notifications.EmailNotificationPort;
import com.psyassistant.notifications.log.EmailDeliveryLog;
import com.psyassistant.notifications.log.EmailDeliveryLogRepository;
import com.psyassistant.notifications.log.EmailDeliveryStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link EmailNotificationPort} implementation that sends via Gmail SMTP.
 *
 * <p>The {@link #queue(EmailMessage)} method persists a {@code PENDING} outbox row in the
 * caller's transaction.  The {@link #send(EmailDeliveryLog)} method is called by
 * {@link com.psyassistant.notifications.outbox.EmailOutboxProcessor} and performs the
 * actual SMTP dispatch.
 *
 * <p><strong>CONFIG_ERROR fail-safe:</strong> if {@code EMAIL_SENDER_ADDRESS},
 * {@code EMAIL_SMTP_USERNAME}, or {@code EMAIL_SMTP_PASSWORD} are blank at queue time,
 * the row is written with {@code CONFIG_ERROR} status, an ERROR is logged (using only
 * the address hash — no plaintext PII), and {@link EmailConfigException} is thrown.
 *
 * <p><strong>PII policy:</strong> {@code recipientAddressEncrypted} must never appear
 * in any log statement. Use {@code recipientAddressHash} for log correlation only.
 */
@Component
public class GmailSmtpAdapter implements EmailNotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(GmailSmtpAdapter.class);
    private static final String SHA_256 = "SHA-256";

    private final JavaMailSender mailSender;
    private final GmailEmailProperties properties;
    private final EmailDeliveryLogRepository repository;
    private final MessageSource messageSource;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    /**
     * Constructs the adapter.
     *
     * @param mailSender     Spring {@link JavaMailSender} configured for Gmail SMTP
     * @param properties     typed configuration properties
     * @param repository     outbox delivery log repository
     * @param messageSource  message source for resolving i18n subject keys
     */
    public GmailSmtpAdapter(
            final JavaMailSender mailSender,
            final GmailEmailProperties properties,
            final EmailDeliveryLogRepository repository,
            final MessageSource messageSource) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.repository = repository;
        this.messageSource = messageSource;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Persists a {@code PENDING} outbox row within the current transaction.
     * If configuration is incomplete, persists {@code CONFIG_ERROR} and throws
     * {@link EmailConfigException}.
     */
    @Override
    @Transactional
    public void queue(final EmailMessage message) {
        final String hash = sha256Hex(message.recipientAddress());
        final EmailDeliveryLog log = new EmailDeliveryLog(
                message.eventType(),
                message.recipientAddress(),  // stored encrypted via EncryptedStringConverter
                hash,
                message.subjectTemplateKey(),
                message.renderedBody());

        if (!isConfigured()) {
            log.setStatus(EmailDeliveryStatus.CONFIG_ERROR);
            log.setLastError("SMTP credentials or sender address not configured");
            repository.save(log);
            LOG.error("EMAIL_CONFIG_ERROR recipientAddressHash={} eventType={} — "
                    + "EMAIL_SMTP_USERNAME, EMAIL_SMTP_PASSWORD or EMAIL_SENDER_ADDRESS is blank",
                    hash, message.eventType());
            throw new EmailConfigException(
                    "Email provider credentials are not configured; email will not be sent");
        }

        repository.save(log);
        LOG.debug("EMAIL_QUEUED logId={} recipientAddressHash={} eventType={}",
                log.getId(), hash, message.eventType());
    }

    /**
     * Dispatches the given log entry via Gmail SMTP and updates its status fields.
     *
     * <p>Called by {@link com.psyassistant.notifications.outbox.EmailOutboxProcessor}.
     * Must be called outside of any outer transaction so the status update is committed
     * independently of the poller's lock.
     *
     * @param entry the delivery log entry in {@code SENDING} state
     */
    @Transactional
    public void send(final EmailDeliveryLog entry) {
        if (!isConfigured()) {
            entry.setStatus(EmailDeliveryStatus.CONFIG_ERROR);
            entry.setLastError("SMTP credentials or sender address not configured at send time");
            repository.save(entry);
            LOG.error("EMAIL_CONFIG_ERROR_AT_SEND logId={} recipientAddressHash={}",
                    entry.getId(), entry.getRecipientAddressHash());
            return;
        }

        try {
            final String subject = messageSource.getMessage(
                    entry.getSubjectTemplateKey(), null, entry.getSubjectTemplateKey(), Locale.ENGLISH);
            final SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.getSenderName() + " <" + properties.getSenderAddress() + ">");
            mail.setTo(entry.getRecipientAddressEncrypted()); // decrypted by converter on read
            mail.setSubject(subject);
            mail.setText(entry.getBody() != null ? entry.getBody() : "");

            mailSender.send(mail);

            // Spring JavaMailSender does not expose the SMTP message-id via SimpleMailMessage;
            // record a synthetic identifier until a MimeMessage-based adapter is implemented.
            final String syntheticId = "smtp-" + entry.getId();
            entry.setProviderMessageId(syntheticId);
            entry.setStatus(EmailDeliveryStatus.SENT);
            entry.setSentAt(Instant.now());
            repository.save(entry);

            LOG.info("EMAIL_SENT logId={} recipientAddressHash={} eventType={}",
                    entry.getId(), entry.getRecipientAddressHash(), entry.getEventType());

        } catch (MailException ex) {
            LOG.warn("EMAIL_SEND_FAILED logId={} recipientAddressHash={} error={}",
                    entry.getId(), entry.getRecipientAddressHash(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Returns {@code true} when all required SMTP credentials are present and non-blank.
     *
     * @return {@code true} if configured
     */
    public boolean isConfigured() {
        return !isBlank(smtpUsername)
                && !isBlank(smtpPassword)
                && !isBlank(properties.getSenderAddress());
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String sha256Hex(final String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(SHA_256);
            final byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
