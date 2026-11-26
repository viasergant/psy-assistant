package com.psyassistant.notifications.telegram;

import com.psyassistant.scheduling.repository.AppointmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final TelegramClient clientBot;
    private final TelegramLinkService linkService;
    private final ClientTelegramLinkRepository clientLinks;
    private final AppointmentRepository appointments;
    private final TelegramMessageFormatter formatter;
    private final TelegramProperties props;
    private final Clock clock;

    /**
     * Creates the processor.
     *
     * @param deliveries  outbox
     * @param settings    settings
     * @param client      therapist bot API
     * @param clientBot   client bot API
     * @param linkService link service (used to unlink blocked chats)
     * @param clientLinks client links
     * @param appointments appointments (to drop reminders for sessions that are no longer upcoming)
     * @param formatter   text source for button labels
     * @param props       configuration
     * @param clock       clock
     */
    public TelegramOutboxProcessor(final TelegramDeliveryLogRepository deliveries,
                                   final TelegramSettingsRepository settings, final TelegramClient client,
                                   @Qualifier(TelegramConfig.CLIENT_BOT) final TelegramClient clientBot,
                                   final TelegramLinkService linkService,
                                   final ClientTelegramLinkRepository clientLinks,
                                   final AppointmentRepository appointments,
                                   final TelegramMessageFormatter formatter, final TelegramProperties props,
                                   final Clock clock) {
        this.deliveries = deliveries;
        this.settings = settings;
        this.client = client;
        this.clientBot = clientBot;
        this.linkService = linkService;
        this.clientLinks = clientLinks;
        this.appointments = appointments;
        this.formatter = formatter;
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
        if (row.getClientId() != null) {
            deliverToClient(row, now);
            return;
        }
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
            handleFailure(row, () -> linkService.unlink(target.get().getUserId()), e, now);
        }
    }

    private void deliverToClient(final TelegramDeliveryLog row, final Instant now) {
        final var target = clientLinks.findByClientId(row.getClientId())
                .filter(l -> l.getOrgId().equals(row.getOrgId()));
        if (target.isEmpty() || row.getAppointmentId() != null && !stillUpcoming(row, now)) {
            row.setStatus(TelegramDeliveryLog.Status.CANCELLED);
            return;
        }
        final ClientTelegramLink link = target.get();
        try {
            if (row.getAppointmentId() == null) {
                clientBot.sendMessage(link.getChatId(), row.getBody());
            } else {
                clientBot.sendMessage(link.getChatId(), row.getBody(), List.of(cancelButton(link,
                        row.getAppointmentId())));
            }
            row.setStatus(TelegramDeliveryLog.Status.SENT);
            row.setSentAt(now);
            row.setLastError(null);
        } catch (TelegramApiException e) {
            handleFailure(row, () -> linkService.unlinkClient(link.getClientId()), e, now);
        }
    }

    /** The session must still exist for this client and organization, and be cancellable. */
    private boolean stillUpcoming(final TelegramDeliveryLog row, final Instant now) {
        return appointments.findById(row.getAppointmentId())
                .filter(a -> row.getOrgId().equals(a.getOrgId()) && row.getClientId().equals(a.getClientId()))
                .filter(a -> ClientSessionCancellationService.cancellable(a, now))
                .isPresent();
    }

    private TelegramClient.Button cancelButton(final ClientTelegramLink link, final UUID appointmentId) {
        return new TelegramClient.Button(
                formatter.text("telegram.client.btn.cancel", Locale.forLanguageTag(link.getLanguage())),
                ClientBotUpdateHandler.CANCEL_ASK + appointmentId);
    }

    private void handleFailure(final TelegramDeliveryLog row, final Runnable unlinkRecipient,
                               final TelegramApiException e, final Instant now) {
        row.setLastError(truncate(e.getMessage()));
        if (e.isRecipientGone()) {
            row.setStatus(TelegramDeliveryLog.Status.FAILED);
            unlinkRecipient.run();
            LOG.info("Telegram recipient unreachable; unlinked");
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
