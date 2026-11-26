package com.psyassistant.notifications.telegram;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.notifications.telegram.TelegramDeliveryLog.Kind;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides which digests and reminders are due and queues them in the outbox.
 *
 * <p>Callers must bind {@code TenantContext} to the settings' organization <em>before</em> invoking
 * these methods so that the {@code orgFilter} is active for every query. Each method only reads the
 * appointments of the therapist who owns the settings row.
 */
@Service
public class TelegramSchedulePlanner {

    private static final Set<AppointmentStatus> INACTIVE =
            Set.of(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW,
                    AppointmentStatus.CLIENT_REJECTED);
    /** A digest is still sent when the server was briefly down at the configured time. */
    private static final Duration DIGEST_GRACE = Duration.ofHours(2);

    private final AppointmentRepository appointments;
    private final TherapistProfileRepository therapists;
    private final ClientRepository clients;
    private final TelegramDeliveryLogRepository deliveries;
    private final TelegramMessageFormatter formatter;
    private final String calendarUrl;

    /**
     * Creates the planner.
     *
     * @param appointments appointment repository
     * @param therapists   therapist repository
     * @param clients      client repository
     * @param deliveries   outbox
     * @param formatter    message formatter
     * @param frontendBaseUrl base URL of the web app
     */
    public TelegramSchedulePlanner(final AppointmentRepository appointments,
                                   final TherapistProfileRepository therapists,
                                   final ClientRepository clients,
                                   final TelegramDeliveryLogRepository deliveries,
                                   final TelegramMessageFormatter formatter,
                                   @Value("${app.frontend-base-url:}") final String frontendBaseUrl) {
        this.appointments = appointments;
        this.therapists = therapists;
        this.clients = clients;
        this.deliveries = deliveries;
        this.formatter = formatter;
        this.calendarUrl = frontendBaseUrl + "/schedule";
    }

    /**
     * Queues today's digest when it is due and has not been queued yet.
     *
     * @param s   the therapist's settings
     * @param now current instant
     * @return true when a digest was queued
     */
    @Transactional
    public boolean planDigest(final TherapistNotificationSettings s, final Instant now) {
        if (!s.isLinked() || !s.isDigestEnabled()) {
            return false;
        }
        final ZoneId zone = zoneOf(s);
        final ZonedDateTime local = now.atZone(zone);
        final LocalTime due = s.getDigestTime();
        final boolean dayEnabled = (s.getDigestDays() & (1 << (local.getDayOfWeek().getValue() - 1))) != 0;
        if (!dayEnabled || local.toLocalTime().isBefore(due)
                || local.toLocalTime().isAfter(due.plus(DIGEST_GRACE))) {
            return false;
        }
        final LocalDate date = local.toLocalDate();
        final String key = "digest:" + s.getUserId() + ":" + date;
        if (deliveries.existsByDedupKey(key)) {
            return false;
        }
        final List<Appointment> day = activeBetween(s, date.atStartOfDay(zone), date.plusDays(1).atStartOfDay(zone));
        if (day.isEmpty() && s.isSkipEmptyDigest()) {
            return false;
        }
        final String text = formatter.digest(date, toLines(s, day), zone, locale(s), calendarUrl);
        deliveries.save(new TelegramDeliveryLog(s.getOrgId(), s.getUserId(), Kind.DIGEST, key, text));
        return true;
    }

    /**
     * Queues reminders for sessions that start within the configured lead time.
     *
     * @param s   the therapist's settings
     * @param now current instant
     * @return number of reminders queued
     */
    @Transactional
    public int planReminders(final TherapistNotificationSettings s, final Instant now) {
        if (!s.isLinked() || !s.isReminderEnabled()) {
            return 0;
        }
        final ZoneId zone = zoneOf(s);
        final ZonedDateTime from = now.atZone(zone);
        final ZonedDateTime to = from.plusMinutes(s.getReminderMinutesBefore());
        final List<Appointment> upcoming = activeBetween(s, from, to);
        int queued = 0;
        final Map<UUID, String> labels = labels(s, upcoming);
        for (final Appointment a : upcoming) {
            final String key = "reminder:" + a.getId() + ":" + a.getStartTime().toEpochSecond();
            if (deliveries.existsByDedupKey(key)) {
                continue;
            }
            final long minutes = Math.max(1, Duration.between(from, a.getStartTime()).toMinutes());
            final String text = formatter.reminder(line(a, labels), zone, locale(s), minutes);
            deliveries.save(new TelegramDeliveryLog(s.getOrgId(), s.getUserId(), Kind.REMINDER, key, text));
            queued++;
        }
        return queued;
    }

    private List<Appointment> activeBetween(final TherapistNotificationSettings s,
                                            final ZonedDateTime from, final ZonedDateTime to) {
        return therapists.findByUserId(s.getUserId())
                .map(p -> appointments.findByTherapistAndDateRange(p.getId(), from, to).stream()
                        .filter(a -> !INACTIVE.contains(a.getStatus()))
                        .toList())
                .orElse(List.of());
    }

    private List<TelegramMessageFormatter.Line> toLines(final TherapistNotificationSettings s,
                                                        final List<Appointment> list) {
        final Map<UUID, String> labels = labels(s, list);
        return list.stream().map(a -> line(a, labels)).toList();
    }

    private TelegramMessageFormatter.Line line(final Appointment a, final Map<UUID, String> labels) {
        return new TelegramMessageFormatter.Line(a.getStartTime(),
                a.getDurationMinutes() == null ? 0 : a.getDurationMinutes(),
                a.getSessionType() == null ? "" : a.getSessionType().getName(),
                labels.get(a.getClientId()));
    }

    private Map<UUID, String> labels(final TherapistNotificationSettings s, final List<Appointment> list) {
        final Set<UUID> ids = list.stream().map(Appointment::getClientId).collect(Collectors.toSet());
        // Client lookups go through the org-filtered repository (TenantContext is bound by the caller).
        final Map<UUID, Client> byId = clients.findAllById(ids).stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        return ids.stream().collect(Collectors.toMap(Function.identity(),
                id -> label(s, byId.get(id), locale(s))));
    }

    static String label(final TherapistNotificationSettings s, final Client client, final Locale locale) {
        if (client == null) {
            return "?";
        }
        if (s.isIncludeClientNames()) {
            final String name = client.getPreferredName() != null && !client.getPreferredName().isBlank()
                    ? client.getPreferredName() : client.getFullName();
            if (name != null && !name.isBlank()) {
                return name.trim().split("\\s+")[0];
            }
        }
        return client.getClientCode() != null
                ? client.getClientCode()
                : "#" + client.getId().toString().substring(0, 8);
    }

    private static ZoneId zoneOf(final TherapistNotificationSettings s) {
        try {
            return ZoneId.of(s.getTimezone());
        } catch (java.time.DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }

    private static Locale locale(final TherapistNotificationSettings s) {
        return Locale.forLanguageTag(s.getLanguage());
    }
}
