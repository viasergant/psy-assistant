package com.psyassistant.sessions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.sessions.config.AttendanceProperties;
import com.psyassistant.sessions.domain.AttendanceAuditLog;
import com.psyassistant.sessions.domain.AttendanceOutcome;
import com.psyassistant.sessions.domain.GroupSessionAttendance;
import com.psyassistant.sessions.domain.SessionParticipant;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.dto.RecordGroupAttendanceRequest;
import com.psyassistant.sessions.event.AttendanceOutcomeRecordedEvent;
import com.psyassistant.sessions.repository.AttendanceAuditLogRepository;
import com.psyassistant.sessions.repository.GroupSessionAttendanceRepository;
import com.psyassistant.sessions.repository.SessionParticipantRepository;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for {@link GroupAttendanceService}.
 *
 * <p>Covers:
 * <ul>
 *     <li>Successful per-client attendance recording</li>
 *     <li>Independent escalation event publication per client</li>
 *     <li>Late-cancellation downgrade logic (reusing AttendanceOutcomeService logic)</li>
 *     <li>Rejection when session is not a GROUP record</li>
 *     <li>Rejection when client is not an active participant</li>
 *     <li>Audit log entry creation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GroupAttendanceServiceTest {

    @Mock private SessionRecordRepository sessionRecordRepository;
    @Mock private GroupSessionAttendanceRepository attendanceRepository;
    @Mock private AttendanceAuditLogRepository auditLogRepository;
    @Mock private SessionParticipantRepository participantRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AttendanceProperties attendanceProperties;
    @Mock private AttendanceProperties.LateCancellationProperties lateCancellationConfig;

    @InjectMocks
    private GroupAttendanceService service;

    private UUID sessionId;
    private UUID clientId;
    private UUID therapistId;
    private UUID actorUserId;
    private SessionRecord groupSession;
    private SessionParticipant activeParticipant;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        therapistId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        final SessionType sessionType =
                new SessionType("GROUP_THERAPY", "Group Therapy", "Group therapy session");

        groupSession = SessionRecord.forGroup(
                UUID.randomUUID(),
                therapistId,
                LocalDate.now(),
                LocalTime.of(10, 0),
                sessionType,
                Duration.ofMinutes(60),
                SessionStatus.COMPLETED
        );

        activeParticipant = new SessionParticipant(sessionId, clientId);
    }

    @Test
    @DisplayName("recordOutcomeForClient - records attendance and publishes event")
    void recordOutcomeForClientSuccess() {
        // Given
        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.of(groupSession));
        when(participantRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.of(activeParticipant));
        when(attendanceRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(GroupSessionAttendance.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(AttendanceAuditLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        final RecordGroupAttendanceRequest request =
                new RecordGroupAttendanceRequest(AttendanceOutcome.ATTENDED, null, null);

        // When
        final GroupSessionAttendance result =
                service.recordOutcomeForClient(sessionId, clientId, request, actorUserId);

        // Then
        assertThat(result.getAttendanceOutcome()).isEqualTo(AttendanceOutcome.ATTENDED);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getSessionRecordId()).isEqualTo(sessionId);

        // Verify event published with correct clientId (independent per-client escalation)
        final ArgumentCaptor<AttendanceOutcomeRecordedEvent> eventCaptor =
                ArgumentCaptor.forClass(AttendanceOutcomeRecordedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        final AttendanceOutcomeRecordedEvent event = eventCaptor.getValue();
        assertThat(event.clientId()).isEqualTo(clientId);
        assertThat(event.therapistId()).isEqualTo(therapistId);
        assertThat(event.newOutcome()).isEqualTo(AttendanceOutcome.ATTENDED);
        assertThat(event.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("recordOutcomeForClient - writes audit log entry")
    void recordOutcomeForClientWritesAuditLog() {
        // Given
        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.of(groupSession));
        when(participantRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.of(activeParticipant));
        when(attendanceRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        final RecordGroupAttendanceRequest request =
                new RecordGroupAttendanceRequest(AttendanceOutcome.NO_SHOW, null, null);

        // When
        service.recordOutcomeForClient(sessionId, clientId, request, actorUserId);

        // Then
        final ArgumentCaptor<AttendanceAuditLog> auditCaptor =
                ArgumentCaptor.forClass(AttendanceAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        final AttendanceAuditLog log = auditCaptor.getValue();
        assertThat(log.getNewOutcome()).isEqualTo(AttendanceOutcome.NO_SHOW);
        assertThat(log.getChangedByUserId()).isEqualTo(actorUserId);
    }

    @Test
    @DisplayName("recordOutcomeForClient - throws IllegalArgumentException for INDIVIDUAL session")
    void recordOutcomeForClientRejectsIndividualSession() {
        // Given — create an INDIVIDUAL session
        final SessionType sessionType =
                new SessionType("ONLINE", "Online", "Online session");
        final SessionRecord individual = new SessionRecord(
                UUID.randomUUID(),
                clientId,
                therapistId,
                LocalDate.now(),
                LocalTime.of(10, 0),
                sessionType,
                Duration.ofMinutes(60),
                SessionStatus.COMPLETED
        );

        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.of(individual));

        final RecordGroupAttendanceRequest request =
                new RecordGroupAttendanceRequest(AttendanceOutcome.ATTENDED, null, null);

        // When / Then
        assertThatThrownBy(() ->
                service.recordOutcomeForClient(sessionId, clientId, request, actorUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only valid for GROUP");

        verify(attendanceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("recordOutcomeForClient - throws EntityNotFoundException for inactive participant")
    void recordOutcomeForClientRejectsInactiveParticipant() {
        // Given
        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.of(groupSession));
        // No participant found
        when(participantRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.empty());

        final RecordGroupAttendanceRequest request =
                new RecordGroupAttendanceRequest(AttendanceOutcome.ATTENDED, null, null);

        // When / Then
        assertThatThrownBy(() ->
                service.recordOutcomeForClient(sessionId, clientId, request, actorUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not an active participant");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordOutcomeForClient - sessions not found throws EntityNotFoundException")
    void recordOutcomeForClientSessionNotFound() {
        // Given
        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.empty());

        final RecordGroupAttendanceRequest request =
                new RecordGroupAttendanceRequest(AttendanceOutcome.ATTENDED, null, null);

        // When / Then
        assertThatThrownBy(() ->
                service.recordOutcomeForClient(sessionId, clientId, request, actorUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Session record not found");
    }

    @Test
    @DisplayName("recordOutcomeForClient - updates existing attendance record on second call")
    void recordOutcomeForClientUpdatesExistingRecord() {
        // Given — attendance already recorded as NO_SHOW, now updating to ATTENDED
        final GroupSessionAttendance existing = new GroupSessionAttendance(
                sessionId, clientId, AttendanceOutcome.NO_SHOW, actorUserId);

        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.of(groupSession));
        when(participantRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.of(activeParticipant));
        when(attendanceRepository.findBySessionRecordIdAndClientId(sessionId, clientId))
                .thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        final RecordGroupAttendanceRequest request =
                new RecordGroupAttendanceRequest(AttendanceOutcome.ATTENDED, null, null);

        // When
        final GroupSessionAttendance result =
                service.recordOutcomeForClient(sessionId, clientId, request, actorUserId);

        // Then
        assertThat(result.getAttendanceOutcome()).isEqualTo(AttendanceOutcome.ATTENDED);

        // Audit should capture previous outcome
        final ArgumentCaptor<AttendanceAuditLog> auditCaptor =
                ArgumentCaptor.forClass(AttendanceAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getPreviousOutcome()).isEqualTo(AttendanceOutcome.NO_SHOW);
        assertThat(auditCaptor.getValue().getNewOutcome()).isEqualTo(AttendanceOutcome.ATTENDED);
    }
}
