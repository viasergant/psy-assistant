package com.psyassistant.notifications.telegram;

import com.psyassistant.notifications.telegram.TelegramDeliveryLog.Kind;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queues session reminders for linked clients.
 *
 * <p>Callers must bind {@code TenantContext} to the link's organization <em>before</em> invoking
 * {@link #plan}, so the org filter is active. Only the appointments of the linked client are read, and the
 * message contains just the time and session type (no therapist, notes or diagnosis).
 */
@Service
public class TelegramClientReminderPlanner {

    private final AppointmentRepository appointments;
    private final TelegramDeliveryLogRepository deliveries;
    private final TelegramMessageFormatter formatter;
    private final TelegramProperties props;

    /**
     * Creates the planner.
     *
     * @param appointments appointment repository
     * @param deliveries   outbox
     * @param formatter    message formatter
     * @param props        configuration
     */
    public TelegramClientReminderPlanner(final AppointmentRepository appointments,
                                         final TelegramDeliveryLogRepository deliveries,
                                         final TelegramMessageFormatter formatter, final TelegramProperties props) {
        this.appointments = appointments;
        this.deliveries = deliveries;
        this.formatter = formatter;
        this.props = props;
    }

    /**
     * Queues a reminder for every cancellable session that starts within the lead time and has none yet.
     *
     * @param link the client's Telegram link
     * @param now  current instant
     * @return number of reminders queued
     */
    @Transactional
    public int plan(final ClientTelegramLink link, final Instant now) {
        final ZoneId zone = zoneOf(link);
        final Locale locale = Locale.forLanguageTag(link.getLanguage());
        final ZonedDateTime from = now.atZone(zone);
        final ZonedDateTime to = from.plusMinutes(props.getClientReminderMinutes());
        final List<Appointment> upcoming = appointments.findByClientIdAndStartTimeBetween(
                link.getClientId(), from, to);
        int queued = 0;
        for (final Appointment a : upcoming) {
            if (!link.getClientId().equals(a.getClientId()) || !ClientSessionCancellationService.cancellable(a, now)) {
                continue;
            }
            final String key = "client-reminder:" + a.getId() + ":" + a.getStartTime().toEpochSecond();
            if (deliveries.existsByDedupKey(key)) {
                continue;
            }
            final String text = formatter.clientReminder(a.getStartTime(),
                    a.getSessionType() == null ? "" : a.getSessionType().getName(), zone, locale);
            deliveries.save(TelegramDeliveryLog.forClient(link.getOrgId(), link.getClientId(), a.getId(),
                    Kind.CLIENT_REMINDER, key, text));
            queued++;
        }
        return queued;
    }

    static ZoneId zoneOf(final ClientTelegramLink link) {
        try {
            return ZoneId.of(link.getTimezone());
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
