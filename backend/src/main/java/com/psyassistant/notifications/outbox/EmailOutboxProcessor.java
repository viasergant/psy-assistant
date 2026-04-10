package com.psyassistant.notifications.outbox;

import com.psyassistant.notifications.alert.AlertPort;
import com.psyassistant.notifications.gmail.GmailSmtpAdapter;
import com.psyassistant.notifications.log.EmailDeliveryLog;
import com.psyassistant.notifications.log.EmailDeliveryLogRepository;
import com.psyassistant.notifications.log.EmailDeliveryStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled outbox poller that dispatches pending email deliveries.
 *
 * <p>Runs every {@code notifications.email.outbox.poll-interval-ms} milliseconds (default 5s).
 * Uses {@code SELECT FOR UPDATE SKIP LOCKED} (via {@link EmailDeliveryLogRepository#findNextBatch})
 * so multiple poller instances can coexist without processing the same row twice.
 *
 * <p>Retry backoff: {@code delay(n) = min(1s * 2^n + rand(0, 500ms), 60s)}.
 * After {@code notifications.email.max-retries} failed attempts, the row is marked
 * {@link EmailDeliveryStatus#FAILED} and {@link AlertPort#raiseDeliveryFailedAlert} is called.
 */
@Component
public class EmailOutboxProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(EmailOutboxProcessor.class);
    private static final long MAX_BACKOFF_SECONDS = 60L;
    private static final long JITTER_MILLIS = 500L;

    private final EmailDeliveryLogRepository repository;
    private final GmailSmtpAdapter smtpAdapter;
    private final AlertPort alertPort;
    private final int maxRetries;
    private final int batchSize;

    /**
     * Constructs the poller.
     *
     * @param repository  outbox delivery log repository
     * @param smtpAdapter Gmail SMTP adapter used for dispatch
     * @param alertPort   alert sink called on permanent failure
     * @param properties  typed configuration (max-retries, batch-size)
     */
    public EmailOutboxProcessor(
            final EmailDeliveryLogRepository repository,
            final GmailSmtpAdapter smtpAdapter,
            final AlertPort alertPort,
            final com.psyassistant.notifications.gmail.GmailEmailProperties properties) {
        this.repository = repository;
        this.smtpAdapter = smtpAdapter;
        this.alertPort = alertPort;
        this.maxRetries = properties.getMaxRetries();
        this.batchSize = properties.getOutbox().getBatchSize();
    }

    /**
     * Polls for pending email deliveries and dispatches them.
     *
     * <p>The fixed-delay scheduler ensures the next run begins only after the current
     * one completes, preventing overlapping runs within a single JVM instance.
     */
    @Scheduled(fixedDelayString = "${notifications.email.outbox.poll-interval-ms:5000}")
    @Transactional
    public void processBatch() {
        final List<EmailDeliveryLog> batch =
                repository.findNextBatch(Instant.now(), batchSize);

        if (batch.isEmpty()) {
            return;
        }

        LOG.debug("EMAIL_OUTBOX_POLLER_BATCH size={}", batch.size());

        for (final EmailDeliveryLog entry : batch) {
            dispatch(entry);
        }
    }

    private void dispatch(final EmailDeliveryLog entry) {
        entry.setStatus(EmailDeliveryStatus.SENDING);
        repository.save(entry);

        try {
            smtpAdapter.send(entry);
        } catch (MailException ex) {
            handleFailure(entry, ex.getMessage());
        } catch (Exception ex) {
            LOG.error("EMAIL_UNEXPECTED_ERROR logId={} recipientAddressHash={} error={}",
                    entry.getId(), entry.getRecipientAddressHash(), ex.getMessage());
            handleFailure(entry, ex.getMessage());
        }
    }

    private void handleFailure(final EmailDeliveryLog entry, final String errorMessage) {
        final int attempts = entry.getRetryCount() + 1;
        entry.setRetryCount(attempts);
        entry.setLastError(truncate(errorMessage, 1000));

        if (attempts >= maxRetries) {
            entry.setStatus(EmailDeliveryStatus.FAILED);
            repository.save(entry);
            LOG.error("EMAIL_MAX_RETRIES_EXCEEDED logId={} recipientAddressHash={} retryCount={}",
                    entry.getId(), entry.getRecipientAddressHash(), attempts);
            alertPort.raiseDeliveryFailedAlert(entry.getId());
        } else {
            entry.setStatus(EmailDeliveryStatus.RETRY);
            entry.setNextRetryAt(computeNextRetryAt(attempts));
            repository.save(entry);
            LOG.warn("EMAIL_WILL_RETRY logId={} recipientAddressHash={} retryCount={} nextRetryAt={}",
                    entry.getId(), entry.getRecipientAddressHash(), attempts, entry.getNextRetryAt());
        }
    }

    /**
     * Computes the next retry timestamp using exponential backoff with jitter.
     *
     * <p>Formula: {@code min(1s * 2^n + rand(0, 500ms), 60s)}.
     *
     * @param attemptNumber the 1-based attempt count after failure
     * @return the instant at which the next attempt may be made
     */
    Instant computeNextRetryAt(final int attemptNumber) {
        final long baseMs = 1000L * (1L << Math.min(attemptNumber, 5));
        final long jitterMs = ThreadLocalRandom.current().nextLong(0, JITTER_MILLIS);
        final long delayMs = Math.min(baseMs + jitterMs, MAX_BACKOFF_SECONDS * 1000L);
        return Instant.now().plus(Duration.ofMillis(delayMs));
    }

    private static String truncate(final String value, final int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
