package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import com.psyassistant.common.tenant.TenantContext;
import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end check (real handler, services, repositories and H2 schema; only the Telegram API is mocked) of the
 * scenario: a therapist has a session, the linked client rejects it from the bot, either late or in good time.
 *
 * <p>The session is kept with status {@code CLIENT_REJECTED}, stays visible to the calendar query, and the
 * therapist gets a Telegram notice that says whether the rejection was late.
 */
@SpringBootTest
@ActiveProfiles("test")
class ClientRejectionIntegrationTest {

    private static final String CLIENT_CHAT = "777001";
    private static final String CONFIRM_REJECT = "cy:";

    @MockitoBean(name = "clientBotClient")
    private TelegramClient clientBot;

    /** The session_record table uses Postgres-only column types and cannot be created on H2. */
    @MockitoBean
    private SessionRecordRepository sessionRecords;

    @Autowired private ClientBotUpdateHandler handler;
    @Autowired private AppointmentRepository appointments;
    @Autowired private SessionTypeRepository sessionTypes;
    @Autowired private TherapistProfileRepository therapists;
    @Autowired private ClientRepository clients;
    @Autowired private ClientTelegramLinkRepository clientLinks;
    @Autowired private TelegramSettingsRepository therapistSettings;
    @Autowired private TelegramDeliveryLogRepository deliveries;

    private UUID orgId;
    private UUID therapistUserId;
    private UUID therapistProfileId;
    private SessionType sessionType;

    @BeforeEach
    void therapistHasLinkedTelegramAndClientIsLinkedToTheBot() {
        orgId = UUID.randomUUID();
        therapistUserId = UUID.randomUUID();
        TenantContext.set(orgId);

        final TherapistProfile therapist = new TherapistProfile("t-" + orgId + "@example.test", "Therapist", null);
        therapist.setOrgId(orgId);
        therapist.setUserId(therapistUserId);
        therapistProfileId = therapists.save(therapist).getId();

        final TherapistNotificationSettings settings = new TherapistNotificationSettings(orgId, therapistUserId);
        settings.link("999001", TelegramLinkService.sha256("999001"), Instant.now());
        therapistSettings.save(settings);

        sessionType = sessionTypes.save(new SessionType("IT_" + orgId, "Individual", null));
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        deliveries.deleteAllInBatch();
        appointments.deleteAllInBatch();
        clientLinks.deleteAllInBatch();
        therapistSettings.deleteAllInBatch();
        clients.deleteAllInBatch();
        therapists.deleteAllInBatch();
        sessionTypes.deleteAllInBatch();
    }

    @Test
    @DisplayName("Rejection in good time keeps the session as CLIENT_REJECTED and notifies the therapist")
    void rejectionInGoodTimeKeepsTheSession() {
        final Appointment session = therapistCreatesSessionStartingIn(72);
        linkClientToBot(session.getClientId());

        clientPressesYesCancel(session);

        final Appointment after = reload(session);
        assertThat(after.getStatus()).isEqualTo(AppointmentStatus.CLIENT_REJECTED);
        assertThat(after.getCancelledAt()).isNull();
        assertThat(visibleOnTheCalendar(session)).isTrue();
        assertThat(therapistNotices()).singleElement()
                .satisfies(body -> assertThat(body).contains("rejected the session").doesNotContain("late notice"));
        verify(clientBot).editMessage(eq(CLIENT_CHAT), anyLong(), anyString(), anyList());
    }

    @Test
    @DisplayName("Late rejection also keeps the session and the therapist's notice says it was late")
    void lateRejectionKeepsTheSessionAndFlagsItAsLate() {
        final Appointment session = therapistCreatesSessionStartingIn(2);
        linkClientToBot(session.getClientId());

        clientPressesYesCancel(session);

        assertThat(reload(session).getStatus()).isEqualTo(AppointmentStatus.CLIENT_REJECTED);
        assertThat(visibleOnTheCalendar(session)).isTrue();
        assertThat(therapistNotices()).singleElement()
                .satisfies(body -> assertThat(body).contains("rejected the session").contains("late notice"));
    }

    @Test
    @DisplayName("A session of another organization cannot be rejected through someone else's chat")
    void anotherOrganizationsSessionIsUntouched() {
        final Appointment session = therapistCreatesSessionStartingIn(72);
        linkClientToBot(session.getClientId());
        final UUID otherOrg = UUID.randomUUID();
        final UUID otherClient = UUID.randomUUID();
        clientLinks.save(new ClientTelegramLink(otherOrg, otherClient, "888002",
                TelegramLinkService.sha256("888002"), "en", "UTC", Instant.now()));

        handler.handleCallback(new TelegramCallback("cb", "888002", "private", 10L, CONFIRM_REJECT + session.getId()));

        assertThat(reload(session).getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(therapistNotices()).isEmpty();
    }

    private Appointment therapistCreatesSessionStartingIn(final int hours) {
        final Client client = new Client("Rejecting Client");
        client.setOrgId(orgId);
        final UUID clientId = clients.save(client).getId();
        final Appointment appointment = new Appointment(therapistProfileId, clientId, sessionType,
                ZonedDateTime.now(ZoneOffset.UTC).plus(hours, ChronoUnit.HOURS), 60, "UTC");
        appointment.setOrgId(orgId);
        return appointments.save(appointment);
    }

    private void linkClientToBot(final UUID clientId) {
        clientLinks.save(new ClientTelegramLink(orgId, clientId, CLIENT_CHAT,
                TelegramLinkService.sha256(CLIENT_CHAT), "en", "UTC", Instant.now()));
    }

    private void clientPressesYesCancel(final Appointment session) {
        handler.handleCallback(new TelegramCallback("cb", CLIENT_CHAT, "private", 10L,
                CONFIRM_REJECT + session.getId()));
    }

    private Appointment reload(final Appointment session) {
        TenantContext.set(orgId);
        return appointments.findById(session.getId()).orElseThrow();
    }

    private boolean visibleOnTheCalendar(final Appointment session) {
        TenantContext.set(orgId);
        final ZonedDateTime from = session.getStartTime().minusDays(1);
        return appointments.findByDateRangeAndTherapists(from, from.plusDays(10), List.of(therapistProfileId))
                .stream().anyMatch(a -> a.getId().equals(session.getId()));
    }

    private List<String> therapistNotices() {
        return deliveries.findAll().stream()
                .filter(d -> therapistUserId.equals(d.getUserId()))
                .map(TelegramDeliveryLog::getBody)
                .toList();
    }
}
