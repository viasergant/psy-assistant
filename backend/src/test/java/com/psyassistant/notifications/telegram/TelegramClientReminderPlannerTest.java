package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;

class TelegramClientReminderPlannerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    private final UUID org = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private AppointmentRepository appointments;
    private TelegramDeliveryLogRepository deliveries;
    private TelegramClientReminderPlanner planner;
    private ClientTelegramLink link;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        deliveries = mock(TelegramDeliveryLogRepository.class);
        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        planner = new TelegramClientReminderPlanner(appointments, deliveries, new TelegramMessageFormatter(source),
                new TelegramProperties());
        link = new ClientTelegramLink(org, clientId, "777", "h", "en", "Europe/Kyiv", NOW);
    }

    private Appointment appointment(final UUID owner, final AppointmentStatus status, final String start) {
        final Appointment a = mock(Appointment.class);
        final SessionType type = mock(SessionType.class);
        when(type.getName()).thenReturn("Individual");
        when(a.getId()).thenReturn(UUID.randomUUID());
        when(a.getClientId()).thenReturn(owner);
        when(a.getSessionType()).thenReturn(type);
        when(a.getStatus()).thenReturn(status);
        when(a.getStartTime()).thenReturn(ZonedDateTime.parse(start));
        return a;
    }

    @Test
    void queuesOneReminderInTheClientsTimezoneAddressedToTheClientOnly() {
        final Appointment a = appointment(clientId, AppointmentStatus.SCHEDULED, "2026-11-27T07:00:00Z");
        when(appointments.findByClientIdAndStartTimeBetween(eq(clientId), any(), any())).thenReturn(List.of(a));

        assertThat(planner.plan(link, NOW)).isEqualTo(1);

        final ArgumentCaptor<TelegramDeliveryLog> row = ArgumentCaptor.forClass(TelegramDeliveryLog.class);
        verify(deliveries).save(row.capture());
        assertThat(row.getValue().getClientId()).isEqualTo(clientId);
        assertThat(row.getValue().getUserId()).isNull();
        assertThat(row.getValue().getOrgId()).isEqualTo(org);
        assertThat(row.getValue().getAppointmentId()).isEqualTo(a.getId());
        assertThat(row.getValue().getKind()).isEqualTo(TelegramDeliveryLog.Kind.CLIENT_REMINDER);
        assertThat(row.getValue().getBody()).contains("09:00").contains("Individual");
    }

    @Test
    void skipsAlreadyQueuedCancelledStartedAndForeignSessions() {
        final Appointment queued = appointment(clientId, AppointmentStatus.SCHEDULED, "2026-11-27T07:00:00Z");
        final Appointment cancelled = appointment(clientId, AppointmentStatus.CANCELLED, "2026-11-27T08:00:00Z");
        final Appointment started = appointment(clientId, AppointmentStatus.SCHEDULED, "2026-11-26T09:00:00Z");
        final Appointment foreign = appointment(UUID.randomUUID(), AppointmentStatus.SCHEDULED,
                "2026-11-27T09:00:00Z");
        when(appointments.findByClientIdAndStartTimeBetween(eq(clientId), any(), any()))
                .thenReturn(List.of(queued, cancelled, started, foreign));
        when(deliveries.existsByDedupKey("client-reminder:" + queued.getId() + ":"
                + queued.getStartTime().toEpochSecond())).thenReturn(true);

        assertThat(planner.plan(link, NOW)).isZero();

        verify(deliveries, never()).save(any());
    }
}
