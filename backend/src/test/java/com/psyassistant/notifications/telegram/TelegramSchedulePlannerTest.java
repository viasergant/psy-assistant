package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;

class TelegramSchedulePlannerTest {

    private final UUID org = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final UUID profileId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private AppointmentRepository appointments;
    private ClientRepository clients;
    private TelegramDeliveryLogRepository deliveries;
    private TelegramSchedulePlanner planner;
    private TherapistNotificationSettings settings;
    private Client client;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        clients = mock(ClientRepository.class);
        deliveries = mock(TelegramDeliveryLogRepository.class);
        final TherapistProfileRepository therapists = mock(TherapistProfileRepository.class);
        final TherapistProfile profile = mock(TherapistProfile.class);
        when(profile.getId()).thenReturn(profileId);
        when(therapists.findByUserId(user)).thenReturn(Optional.of(profile));
        client = mock(Client.class);
        when(client.getId()).thenReturn(clientId);
        when(client.getClientCode()).thenReturn("C-1042");
        when(client.getFullName()).thenReturn("Olena Kovalenko");
        when(clients.findAllById(any())).thenReturn(List.of(client));
        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        planner = new TelegramSchedulePlanner(appointments, therapists, clients, deliveries,
                new TelegramMessageFormatter(source), "https://app");
        settings = new TherapistNotificationSettings(org, user);
        settings.link("555", "hash", Instant.EPOCH);
        settings.setTimezone("Europe/Kyiv");
        settings.setDigestTime(LocalTime.of(7, 30));
    }

    private Appointment appointment(final String startUtc, final AppointmentStatus status) {
        final Appointment a = mock(Appointment.class);
        final SessionType type = mock(SessionType.class);
        when(type.getName()).thenReturn("Individual");
        when(a.getId()).thenReturn(UUID.randomUUID());
        when(a.getClientId()).thenReturn(clientId);
        when(a.getSessionType()).thenReturn(type);
        when(a.getStartTime()).thenReturn(ZonedDateTime.parse(startUtc));
        when(a.getDurationMinutes()).thenReturn(50);
        when(a.getStatus()).thenReturn(status);
        return a;
    }

    private void givenAppointments(final Appointment... list) {
        when(appointments.findByTherapistAndDateRange(any(), any(), any())).thenReturn(List.of(list));
    }

    private TelegramDeliveryLog queued() {
        final ArgumentCaptor<TelegramDeliveryLog> c = ArgumentCaptor.forClass(TelegramDeliveryLog.class);
        verify(deliveries).save(c.capture());
        return c.getValue();
    }

    @Test
    void digestIsQueuedAtLocalTimeNotUtc() {
        givenAppointments(appointment("2026-11-26T08:00:00Z", AppointmentStatus.SCHEDULED));

        // 05:29Z = 07:29 Kyiv -> not yet; 05:30Z = 07:30 Kyiv -> due
        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T05:29:00Z"))).isFalse();
        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T05:30:00Z"))).isTrue();

        final TelegramDeliveryLog row = queued();
        assertThat(row.getDedupKey()).isEqualTo("digest:" + user + ":2026-11-26");
        assertThat(row.getOrgId()).isEqualTo(org);
        assertThat(row.getUserId()).isEqualTo(user);
        assertThat(row.getBody()).contains("10:00-10:50 | Individual | C-1042");
    }

    @Test
    void digestDefaultsToClientCodeAndNeverLeaksNameUnlessOptedIn() {
        givenAppointments(appointment("2026-11-26T08:00:00Z", AppointmentStatus.SCHEDULED));
        planner.planDigest(settings, Instant.parse("2026-11-26T05:30:00Z"));
        assertThat(queued().getBody()).doesNotContain("Olena").doesNotContain("Kovalenko");
    }

    @Test
    void optInShowsFirstNameOnly() {
        settings.setIncludeClientNames(true);
        givenAppointments(appointment("2026-11-26T08:00:00Z", AppointmentStatus.SCHEDULED));

        planner.planDigest(settings, Instant.parse("2026-11-26T05:30:00Z"));

        assertThat(queued().getBody()).contains("Olena").doesNotContain("Kovalenko");
    }

    @Test
    void digestIsNotQueuedTwiceForTheSameLocalDate() {
        when(deliveries.existsByDedupKey("digest:" + user + ":2026-11-26")).thenReturn(true);

        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T05:31:00Z"))).isFalse();
        verify(deliveries, never()).save(any());
    }

    @Test
    void digestRespectsDisabledDaysSkipEmptyAndGraceWindow() {
        givenAppointments();
        // empty day is skipped by default
        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T05:30:00Z"))).isFalse();
        // Thursday disabled (bit 3)
        settings.setSkipEmptyDigest(false);
        settings.setDigestDays(127 & ~(1 << 3));
        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T05:30:00Z"))).isFalse();
        // more than 2h after the configured time -> too late
        settings.setDigestDays(127);
        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T08:00:00Z"))).isFalse();
        verify(deliveries, never()).save(any());
    }

    @Test
    void digestDisabledOrUnlinkedQueuesNothing() {
        settings.setDigestEnabled(false);
        assertThat(planner.planDigest(settings, Instant.parse("2026-11-26T05:30:00Z"))).isFalse();
        final var unlinked = new TherapistNotificationSettings(org, user);
        assertThat(planner.planDigest(unlinked, Instant.parse("2026-11-26T05:30:00Z"))).isFalse();
        assertThat(planner.planReminders(unlinked, Instant.parse("2026-11-26T05:30:00Z"))).isZero();
    }

    @Test
    void reminderQueuedOnceAndOnlyForActiveSessions() {
        final Appointment active = appointment("2026-11-26T08:00:00Z", AppointmentStatus.CONFIRMED);
        final Appointment done = appointment("2026-11-26T08:10:00Z", AppointmentStatus.COMPLETED);
        final Appointment seen = appointment("2026-11-26T08:20:00Z", AppointmentStatus.SCHEDULED);
        when(deliveries.existsByDedupKey("reminder:" + seen.getId() + ":" + seen.getStartTime().toEpochSecond()))
                .thenReturn(true);
        givenAppointments(active, done, seen);

        final int queued = planner.planReminders(settings, Instant.parse("2026-11-26T07:00:00Z"));

        assertThat(queued).isEqualTo(1);
        final TelegramDeliveryLog row = queued();
        assertThat(row.getDedupKey()).startsWith("reminder:" + active.getId());
        assertThat(row.getBody()).isEqualTo("Session in 60 min: 10:00 | Individual | C-1042");
    }

    @Test
    void reminderDisabledQueuesNothing() {
        settings.setReminderEnabled(false);

        assertThat(planner.planReminders(settings, Instant.parse("2026-11-26T07:00:00Z"))).isZero();
        verify(appointments, never()).findByTherapistAndDateRange(any(), any(), any());
        verify(deliveries, never()).existsByDedupKey(anyString());
    }
}
