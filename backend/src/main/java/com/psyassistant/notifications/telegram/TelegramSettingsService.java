package com.psyassistant.notifications.telegram;

import com.psyassistant.common.tenant.TenantContext;
import com.psyassistant.notifications.telegram.TelegramDeliveryLog.Kind;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reads and updates the caller's own Telegram settings. Every method is scoped to the authenticated user
 * id and the org-filtered repository, so one user can never touch another's settings.
 */
@Service
public class TelegramSettingsService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final TelegramSettingsRepository repository;
    private final TelegramDeliveryLogRepository deliveries;
    private final TelegramMessageFormatter formatter;
    private final TelegramProperties props;

    /**
     * Creates the service.
     *
     * @param repository settings repository
     * @param deliveries outbox
     * @param formatter  text source
     * @param props      configuration
     */
    public TelegramSettingsService(final TelegramSettingsRepository repository,
                                   final TelegramDeliveryLogRepository deliveries,
                                   final TelegramMessageFormatter formatter, final TelegramProperties props) {
        this.repository = repository;
        this.deliveries = deliveries;
        this.formatter = formatter;
        this.props = props;
    }

    /**
     * Returns the caller's settings, or defaults when none are stored yet.
     *
     * @param userId authenticated user
     * @return settings view
     */
    @Transactional(readOnly = true)
    public TelegramSettingsDto get(final UUID userId) {
        return toDto(repository.findByUserId(userId)
                .orElseGet(() -> new TherapistNotificationSettings(TenantContext.require(), userId)));
    }

    /**
     * Updates preferences; creates the settings row on first use.
     *
     * @param userId  authenticated user
     * @param request new values
     * @return updated settings view
     */
    @Transactional
    public TelegramSettingsDto update(final UUID userId, final UpdateTelegramSettingsRequest request) {
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown timezone");
        }
        final TherapistNotificationSettings s = repository.findByUserId(userId)
                .orElseGet(() -> new TherapistNotificationSettings(TenantContext.require(), userId));
        s.setTimezone(request.timezone());
        s.setLanguage(request.language());
        s.setDigestEnabled(request.digestEnabled());
        s.setDigestTime(request.digestTime().withSecond(0).withNano(0));
        s.setDigestDays(request.digestDays());
        s.setSkipEmptyDigest(request.skipEmptyDigest());
        s.setReminderEnabled(request.reminderEnabled());
        s.setReminderMinutesBefore(request.reminderMinutesBefore());
        s.setIncludeClientNames(request.includeClientNames());
        return toDto(repository.save(s));
    }

    /**
     * Queues a test message to the caller's own linked chat.
     *
     * @param userId authenticated user
     */
    @Transactional
    public void sendTest(final UUID userId) {
        final TherapistNotificationSettings s = repository.findByUserId(userId)
                .filter(TherapistNotificationSettings::isLinked)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Telegram is not linked"));
        deliveries.save(new TelegramDeliveryLog(s.getOrgId(), userId, Kind.SYSTEM,
                "test:" + userId + ":" + UUID.randomUUID(),
                formatter.text("telegram.test", Locale.forLanguageTag(s.getLanguage()))));
    }

    private TelegramSettingsDto toDto(final TherapistNotificationSettings s) {
        return new TelegramSettingsDto(props.isOperational(), s.isLinked(), s.getLinkedAt(), s.getTimezone(),
                s.getLanguage(), s.isDigestEnabled(), s.getDigestTime().format(HH_MM), s.getDigestDays(),
                s.isSkipEmptyDigest(), s.isReminderEnabled(), s.getReminderMinutesBefore(),
                s.isIncludeClientNames());
    }
}
