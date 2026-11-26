package com.psyassistant.notifications.telegram;

import com.psyassistant.common.tenant.TenantContext;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every minute, queues due daily digests and session reminders for all linked therapists.
 *
 * <p>The cross-organization scan only reads settings rows; the tenant is then bound per row so every
 * further query is org-filtered. Dedup keys in the outbox make this idempotent and safe on several
 * instances.
 */
@Component
@ConditionalOnProperty(name = "notifications.telegram.enabled", havingValue = "true")
public class TelegramScheduleJob {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramScheduleJob.class);

    private final TelegramSettingsRepository settings;
    private final TelegramSchedulePlanner planner;
    private final Clock clock;

    /**
     * Creates the job.
     *
     * @param settings settings repository
     * @param planner  planner
     * @param clock    clock
     */
    public TelegramScheduleJob(final TelegramSettingsRepository settings, final TelegramSchedulePlanner planner,
                               final Clock clock) {
        this.settings = settings;
        this.planner = planner;
        this.clock = clock;
    }

    /** Scheduled entry point. */
    @Scheduled(fixedDelayString = "${notifications.telegram.schedule-poll-ms:60000}")
    public void run() {
        final var now = clock.instant();
        for (final TherapistNotificationSettings s : settings.findByChatIdHashIsNotNull()) {
            TenantContext.set(s.getOrgId());
            try {
                planner.planDigest(s, now);
                planner.planReminders(s, now);
            } catch (RuntimeException e) {
                // One therapist's failure (or a duplicate-key race with another instance) must not stop the rest.
                LOG.warn("Telegram scheduling failed for user {}: {}", s.getUserId(), e.getClass().getSimpleName());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
