package com.psyassistant.notifications.telegram;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.notifications.telegram.TelegramDeliveryLog.Kind;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.service.AppointmentService;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets a Telegram-linked client cancel their own upcoming session from a bot button.
 *
 * <p>The chat, and through it the client and organization, comes from the stored link, never from the
 * button payload: the payload only names an appointment, which must belong to that client and
 * organization. Callers bind {@code TenantContext} to the link's organization first. Every method opens a
 * <em>new</em> transaction so the org filter is active even when the caller (the polling runner) already
 * runs inside a transaction, and so a failure here cannot mark that outer transaction rollback-only.
 */
@Service
public class ClientSessionCancellationService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSessionCancellationService.class);
    private static final Set<AppointmentStatus> CANCELLABLE =
            Set.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);
    private static final String ACTOR_NAME = "Client (Telegram)";
    private static final String REASON = "Cancelled by the client via Telegram";

    /** Which buttons accompany a reply. */
    public enum Buttons { NONE, CANCEL, CONFIRM }

    /**
     * What the bot shows after a button press.
     *
     * @param text    message text
     * @param buttons buttons to show under it
     */
    public record Reply(String text, Buttons buttons) { }

    private final AppointmentRepository appointments;
    private final AppointmentService appointmentService;
    private final TherapistProfileRepository therapists;
    private final ClientRepository clients;
    private final TelegramSettingsRepository therapistSettings;
    private final TelegramDeliveryLogRepository deliveries;
    private final TelegramMessageFormatter formatter;
    private final TelegramProperties props;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param appointments       appointment repository
     * @param appointmentService appointment domain service (performs the actual cancellation)
     * @param therapists         therapist repository
     * @param clients            client repository
     * @param therapistSettings  therapist Telegram settings
     * @param deliveries         outbox
     * @param formatter          message formatter
     * @param props              configuration
     * @param clock              clock
     */
    public ClientSessionCancellationService(final AppointmentRepository appointments,
                                            final AppointmentService appointmentService,
                                            final TherapistProfileRepository therapists,
                                            final ClientRepository clients,
                                            final TelegramSettingsRepository therapistSettings,
                                            final TelegramDeliveryLogRepository deliveries,
                                            final TelegramMessageFormatter formatter,
                                            final TelegramProperties props, final Clock clock) {
        this.appointments = appointments;
        this.appointmentService = appointmentService;
        this.therapists = therapists;
        this.clients = clients;
        this.therapistSettings = therapistSettings;
        this.deliveries = deliveries;
        this.formatter = formatter;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Returns whether a session can still be cancelled by the client.
     *
     * @param appointment the session
     * @param now         current instant
     * @return true for a scheduled or confirmed session that has not started
     */
    public static boolean cancellable(final Appointment appointment, final Instant now) {
        return CANCELLABLE.contains(appointment.getStatus()) && appointment.getStartTime().toInstant().isAfter(now);
    }

    /**
     * First press on "Cancel session": asks for confirmation, warning when the cancellation is late.
     *
     * @param link          the client's link (identifies client and organization)
     * @param appointmentId appointment named by the button
     * @return the reply to show
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Reply askConfirmation(final ClientTelegramLink link, final UUID appointmentId) {
        final Locale locale = Locale.forLanguageTag(link.getLanguage());
        return owned(link, appointmentId).map(a -> {
            final String when = formatter.clientWhen(a.getStartTime(), TelegramClientReminderPlanner.zoneOf(link),
                    locale);
            final String key = isLate(a) ? "telegram.client.confirm.late" : "telegram.client.confirm";
            return new Reply(formatter.text(key, locale, when, props.getLateCancellationHours()), Buttons.CONFIRM);
        }).orElseGet(() -> unavailable(locale));
    }

    /**
     * "No, keep it": restores the original reminder with its cancel button.
     *
     * @param link          the client's link
     * @param appointmentId appointment named by the button
     * @return the reply to show
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Reply keep(final ClientTelegramLink link, final UUID appointmentId) {
        final Locale locale = Locale.forLanguageTag(link.getLanguage());
        return owned(link, appointmentId)
                .map(a -> new Reply(formatter.clientReminder(a.getStartTime(),
                        a.getSessionType() == null ? "" : a.getSessionType().getName(),
                        TelegramClientReminderPlanner.zoneOf(link), locale), Buttons.CANCEL))
                .orElseGet(() -> unavailable(locale));
    }

    /**
     * "Yes, cancel": cancels the session, drops its pending reminders and tells the therapist.
     *
     * @param link          the client's link
     * @param appointmentId appointment named by the button
     * @return the reply to show
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reply cancel(final ClientTelegramLink link, final UUID appointmentId) {
        final Locale locale = Locale.forLanguageTag(link.getLanguage());
        final Optional<Appointment> found = owned(link, appointmentId);
        final Optional<TherapistProfile> therapist = found
                .flatMap(a -> therapists.findById(a.getTherapistProfileId()));
        if (found.isEmpty() || therapist.isEmpty()) {
            return unavailable(locale);
        }
        final Appointment a = found.get();
        final CancellationType type = isLate(a) ? CancellationType.LATE_CANCELLATION
                : CancellationType.CLIENT_INITIATED;
        // The audit trail and the cancelled_by column reference a user; the client has none, so the action is
        // recorded under the session's therapist with the actor name "Client (Telegram)".
        appointmentService.cancelAppointment(a.getId(), type, REASON, therapist.get().getUserId(), ACTOR_NAME);
        deliveries.cancelPendingForAppointment(a.getId());
        notifyTherapist(link, a, therapist.get(), type);
        LOG.info("Client cancelled appointment {} via Telegram, type={}", a.getId(), type);
        return new Reply(formatter.text("telegram.client.cancelled", locale,
                formatter.clientWhen(a.getStartTime(), TelegramClientReminderPlanner.zoneOf(link), locale)),
                Buttons.NONE);
    }

    private Optional<Appointment> owned(final ClientTelegramLink link, final UUID appointmentId) {
        return appointments.findById(appointmentId)
                .filter(a -> link.getOrgId().equals(a.getOrgId()))
                .filter(a -> link.getClientId().equals(a.getClientId()))
                .filter(a -> cancellable(a, clock.instant()));
    }

    private boolean isLate(final Appointment a) {
        return Duration.between(clock.instant(), a.getStartTime().toInstant())
                .compareTo(Duration.ofHours(props.getLateCancellationHours())) < 0;
    }

    private Reply unavailable(final Locale locale) {
        return new Reply(formatter.text("telegram.client.unavailable", locale), Buttons.NONE);
    }

    private void notifyTherapist(final ClientTelegramLink link, final Appointment a, final TherapistProfile therapist,
                                 final CancellationType type) {
        therapistSettings.findByUserId(therapist.getUserId())
                .filter(s -> s.isLinked() && s.getOrgId().equals(link.getOrgId()))
                .ifPresent(s -> {
                    final Locale locale = Locale.forLanguageTag(s.getLanguage());
                    final Client client = clients.findById(link.getClientId()).orElse(null);
                    final String key = type == CancellationType.LATE_CANCELLATION
                            ? "telegram.therapist.client-cancelled.late" : "telegram.therapist.client-cancelled";
                    final String text = formatter.text(key, locale,
                            TelegramSchedulePlanner.label(s, client, locale),
                            formatter.clientWhen(a.getStartTime(), zone(s), locale));
                    deliveries.save(new TelegramDeliveryLog(s.getOrgId(), s.getUserId(), Kind.SYSTEM,
                            "client-cancel:" + a.getId(), text));
                });
    }

    private static ZoneId zone(final TherapistNotificationSettings s) {
        try {
            return ZoneId.of(s.getTimezone());
        } catch (java.time.DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
