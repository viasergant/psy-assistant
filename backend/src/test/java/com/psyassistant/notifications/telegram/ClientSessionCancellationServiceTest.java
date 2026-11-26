package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.service.AppointmentService;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;

class ClientSessionCancellationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    private final UUID org = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID therapistUser = UUID.randomUUID();
    private final UUID profileId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    private AppointmentRepository appointments;
    private AppointmentService appointmentService;
    private TelegramSettingsRepository therapistSettings;
    private TelegramDeliveryLogRepository deliveries;
    private ClientSessionCancellationService service;
    private ClientTelegramLink link;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        appointmentService = mock(AppointmentService.class);
        therapistSettings = mock(TelegramSettingsRepository.class);
        deliveries = mock(TelegramDeliveryLogRepository.class);
        final TherapistProfileRepository therapists = mock(TherapistProfileRepository.class);
        final TherapistProfile profile = mock(TherapistProfile.class);
        when(profile.getUserId()).thenReturn(therapistUser);
        when(therapists.findById(profileId)).thenReturn(Optional.of(profile));
        final ClientRepository clients = mock(ClientRepository.class);
        final Client client = mock(Client.class);
        when(client.getId()).thenReturn(clientId);
        when(client.getClientCode()).thenReturn("C-1042");
        when(clients.findById(clientId)).thenReturn(Optional.of(client));
        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        service = new ClientSessionCancellationService(appointments, appointmentService, therapists, clients,
                therapistSettings, deliveries, new TelegramMessageFormatter(source), new TelegramProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        link = new ClientTelegramLink(org, clientId, "777", "h", "en", "UTC", NOW);
        appointment = appointment(clientId, org, AppointmentStatus.SCHEDULED, "2026-11-28T10:00:00Z");
        when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
    }

    private Appointment appointment(final UUID owner, final UUID orgId, final AppointmentStatus status,
                                    final String start) {
        final Appointment a = mock(Appointment.class);
        final SessionType type = mock(SessionType.class);
        when(type.getName()).thenReturn("Individual");
        when(a.getId()).thenReturn(appointmentId);
        when(a.getClientId()).thenReturn(owner);
        when(a.getOrgId()).thenReturn(orgId);
        when(a.getTherapistProfileId()).thenReturn(profileId);
        when(a.getSessionType()).thenReturn(type);
        when(a.getStatus()).thenReturn(status);
        when(a.getStartTime()).thenReturn(ZonedDateTime.parse(start));
        return a;
    }

    @Test
    void firstPressAsksForConfirmation() {
        final var reply = service.askConfirmation(link, appointmentId);

        assertThat(reply.buttons()).isEqualTo(ClientSessionCancellationService.Buttons.CONFIRM);
        assertThat(reply.text()).startsWith("Cancel your session on").contains("10:00");
        verify(appointmentService, never()).cancelAppointment(any(), any(), anyString(), any(), anyString());
    }

    @Test
    void lateCancellationIsWarnedAboutBeforeConfirming() {
        when(appointment.getStartTime()).thenReturn(ZonedDateTime.parse("2026-11-26T20:00:00Z"));

        assertThat(service.askConfirmation(link, appointmentId).text()).contains("late cancellation");
    }

    @Test
    void confirmedCancellationIsRecordedAsClientInitiatedAndTheTherapistIsNotified() {
        final TherapistNotificationSettings s = new TherapistNotificationSettings(org, therapistUser);
        s.link("555", "hash", NOW);
        when(therapistSettings.findByUserId(therapistUser)).thenReturn(Optional.of(s));

        final var reply = service.cancel(link, appointmentId);

        verify(appointmentService).cancelAppointment(eq(appointmentId), eq(CancellationType.CLIENT_INITIATED),
                anyString(), eq(therapistUser), eq("Client (Telegram)"));
        verify(deliveries).cancelPendingForAppointment(appointmentId);
        assertThat(reply.buttons()).isEqualTo(ClientSessionCancellationService.Buttons.NONE);
        assertThat(reply.text()).contains("has been cancelled");
        final ArgumentCaptor<TelegramDeliveryLog> notice = ArgumentCaptor.forClass(TelegramDeliveryLog.class);
        verify(deliveries).save(notice.capture());
        assertThat(notice.getValue().getUserId()).isEqualTo(therapistUser);
        assertThat(notice.getValue().getClientId()).isNull();
        assertThat(notice.getValue().getBody()).contains("C-1042").doesNotContain("Olena");
    }

    @Test
    void cancellationInsideTheLateWindowIsClassifiedAsLate() {
        when(appointment.getStartTime()).thenReturn(ZonedDateTime.parse("2026-11-26T20:00:00Z"));

        service.cancel(link, appointmentId);

        verify(appointmentService).cancelAppointment(eq(appointmentId), eq(CancellationType.LATE_CANCELLATION),
                anyString(), eq(therapistUser), anyString());
    }

    @Test
    void anotherClientsOrOrganizationsSessionCannotBeTouched() {
        final Appointment otherClient = appointment(UUID.randomUUID(), org, AppointmentStatus.SCHEDULED,
                "2026-11-28T10:00:00Z");
        when(appointments.findById(appointmentId)).thenReturn(Optional.of(otherClient));
        assertThat(service.cancel(link, appointmentId).text()).contains("can no longer be changed");

        final Appointment otherOrg = appointment(clientId, UUID.randomUUID(), AppointmentStatus.SCHEDULED,
                "2026-11-28T10:00:00Z");
        when(appointments.findById(appointmentId)).thenReturn(Optional.of(otherOrg));
        assertThat(service.cancel(link, appointmentId).text()).contains("can no longer be changed");

        verify(appointmentService, never()).cancelAppointment(any(), any(), anyString(), any(), anyString());
    }

    @Test
    void cancelledStartedOrMissingSessionsAreUnavailable() {
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);
        assertThat(service.cancel(link, appointmentId).text()).contains("can no longer be changed");

        when(appointment.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(appointment.getStartTime()).thenReturn(ZonedDateTime.parse("2026-11-26T09:00:00Z"));
        assertThat(service.askConfirmation(link, appointmentId).text()).contains("can no longer be changed");

        when(appointments.findById(appointmentId)).thenReturn(Optional.empty());
        assertThat(service.keep(link, appointmentId).text()).contains("can no longer be changed");

        verify(appointmentService, never()).cancelAppointment(any(), any(), anyString(), any(), anyString());
    }

    @Test
    void decliningRestoresTheReminderWithItsCancelButton() {
        final var reply = service.keep(link, appointmentId);

        assertThat(reply.buttons()).isEqualTo(ClientSessionCancellationService.Buttons.CANCEL);
        assertThat(reply.text()).startsWith("Reminder: you have a session on");
    }
}
