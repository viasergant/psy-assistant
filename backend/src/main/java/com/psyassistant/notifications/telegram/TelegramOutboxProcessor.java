package com.psyassistant.notifications.telegram;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivers queued Telegram messages. Rows are claimed with SKIP LOCKED, so several instances are safe.
 * The recipient chat is resolved from the owner's settings row at send time.
 */
@Component
@ConditionalOnProperty(name = "notifications.telegram.enabled", havingValue = "true")
public class TelegramOutboxProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramOutboxProcessor.class);
    private static final long MAX_BACKOFF_SECONDS = 600;

    private final TelegramDeliveryLogRepository deliveries;
    private final TelegramSettingsRepository settings;
    private final TelegramClient client;
    private final TelegramLinkService linkService;
    private final TelegramProperties props;
    private final Clock clock;

    /**
     * Creates the processor.
     *
     * @param deliveries  outbox
     * @param settings    settings
     * @param client      Telegram API
     * @param linkService link service (used to unlink blocked chats)
     * @param props       configuration
     * @param clock       clock
     */
    public TelegramOutboxProcessor(final TelegramDeliveryLogRepository deliveries,
                                   final TelegramSettingsRepository settings, final TelegramClient client,
                                   final TelegramLinkService linkService, final TelegramProperties props,
                                   final Clock clock) {
        this.deliveries = deliveries;
        this.settings = settings;
        this.client = client;
        this.linkService = linkService;
        this.props = props;
        this.clock = clock;
    }

    /** Scheduled entry point. */
    @Scheduled(fixedDelayString = "${notifications.telegram.outbox-poll-ms:5000}")
    @Transactional
    public void processBatch() {
        final Instant now = clock.instant();
        for (final TelegramDeliveryLog row : deliveries.findNextBatch(now, props.getOutboxBatchSize())) {
            deliver(row, now);
        }
    }

    private void deliver(final TelegramDeliveryLog row, final Instant now) {
        final var target = settings.findByUserId(row.getUserId())
                .filter(s -> s.isLinked() && s.getOrgId().equals(row.getOrgId()));
        if (target.isEmpty()) {
            row.setStatus(TelegramDeliveryLog.Status.CANCELLED);
            return;
        }
        try {
            client.sendMessage(target.get().getChatId(), row.getBody());
            row.setStatus(TelegramDeliveryLog.Status.SENT);
            row.setSentAt(now);
            row.setLastError(null);
        } catch (TelegramApiException e) {
            handleFailure(row, target.get(), e, now);
        }
    }

    private void handleFailure(final TelegramDeliveryLog row, final TherapistNotificationSettings s,
                               final TelegramApiException e, final Instant now) {
        row.setLastError(truncate(e.getMessage()));
        if (e.isRecipientGone()) {
            row.setStatus(TelegramDeliveryLog.Status.FAILED);
            linkService.unlink(s.getUserId());
            LOG.info("Telegram recipient unreachable; unlinked user {}", s.getUserId());
            return;
        }
        final int attempt = row.getRetryCount() + 1;
        row.setRetryCount(attempt);
        if (!e.isRetryable() || attempt >= props.getMaxRetries()) {
            row.setStatus(TelegramDeliveryLog.Status.FAILED);
            LOG.warn("Telegram delivery {} failed permanently", row.getId());
            return;
        }
        final long backoff = e.getRetryAfterSeconds() > 0 ? e.getRetryAfterSeconds()
                : Math.min(MAX_BACKOFF_SECONDS, (1L << attempt) * 5 + ThreadLocalRandom.current().nextLong(5));
        row.setStatus(TelegramDeliveryLog.Status.RETRY);
        row.setNextRetryAt(now.plusSeconds(backoff));
    }

    private static String truncate(final String s) {
        return s == null ? null : s.substring(0, Math.min(255, s.length()));
    }
}
