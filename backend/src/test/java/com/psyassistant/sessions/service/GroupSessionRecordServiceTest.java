package com.psyassistant.sessions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.sessions.domain.RecordKind;
import com.psyassistant.sessions.domain.SessionParticipant;
import com.psyassistant.sessions.domain.SessionParticipantAudit;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.repository.SessionParticipantAuditRepository;
import com.psyassistant.sessions.repository.SessionParticipantRepository;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
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

/**
 * Unit tests for {@link GroupSessionRecordService}.
 *
 * <p>Covers:
 * <ul>
 *     <li>Successful group session creation with participant bulk-insert + audit entries</li>
 *     <li>Participant cap enforcement (≤ 20)</li>
 *     <li>Minimum participant count validation (≥ 2)</li>
 *     <li>Duplicate session detection</li>
 *     <li>Appointment not found</li>
 *     <li>GROUP record_kind is set on the saved entity</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GroupSessionRecordServiceTest {

    @Mock private SessionRecordRepository sessionRecordRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private SessionParticipantRepository participantRepository;
    @Mock private SessionParticipantAuditRepository auditRepository;

    @InjectMocks
    private GroupSessionRecordService service;

    private UUID appointmentId;
    private UUID therapistId;
    private UUID actorUserId;
    private Appointment appointment;
    private SessionType sessionType;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        therapistId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        sessionType = new SessionType("GROUP_THERAPY", "Group Therapy", "Group therapy session");

        appointment = new Appointment(
                therapistId,
                UUID.randomUUID(), // single clientId placeholder — group session ignores this
                sessionType,
                ZonedDateTime.now().plusDays(1),
                60,
                "UTC"
        );
    }

    @Test
    @DisplayName("createGroupSession - creates GROUP session with participants and audit entries")
    void createGroupSessionSuccess() {
        // Given
        final List<UUID> clientIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(participantRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(auditRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        final SessionRecord result = service.createGroupSession(
                appointmentId, clientIds, SessionStatus.PENDING, actorUserId, "Test User");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecordKind()).isEqualTo(RecordKind.GROUP);
        assertThat(result.getClientId()).isNull(); // GROUP sessions have no single clientId
        assertThat(result.getTherapistId()).isEqualTo(therapistId);
        assertThat(result.getStatus()).isEqualTo(SessionStatus.PENDING);
        assertThat(result.getAppointmentId()).isEqualTo(appointmentId);

        // Verify bulk save of participants
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<SessionParticipant>> participantCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(participantCaptor.capture());
        assertThat(participantCaptor.getValue()).hasSize(3);

        // Verify bulk save of audit entries
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<SessionParticipantAudit>> auditCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).hasSize(3);
        assertThat(auditCaptor.getValue())
                .allMatch(a -> a.getAction() == SessionParticipantAudit.Action.ADDED);
    }

    @Test
    @DisplayName("createGroupSession - enforces minimum 2 participants")
    void createGroupSessionRejectsTooFewParticipants() {
        // Given
        final List<UUID> singleClient = List.of(UUID.randomUUID());

        // When / Then
        assertThatThrownBy(() -> service.createGroupSession(
                appointmentId, singleClient, SessionStatus.PENDING, actorUserId, "Test User"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 participants");

        verify(sessionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroupSession - enforces maximum 20 participant cap")
    void createGroupSessionRejectsTooManyParticipants() {
        // Given — 21 participants
        final List<UUID> oversizedGroup =
                java.util.stream.Stream.generate(UUID::randomUUID).limit(21).toList();

        // When / Then
        assertThatThrownBy(() -> service.createGroupSession(
                appointmentId, oversizedGroup, SessionStatus.PENDING, actorUserId, "Test User"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 20");

        verify(sessionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroupSession - throws IllegalStateException if session already exists")
    void createGroupSessionThrowsIfDuplicate() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(true);
        final List<UUID> clientIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // When / Then
        assertThatThrownBy(() -> service.createGroupSession(
                appointmentId, clientIds, SessionStatus.PENDING, actorUserId, "Test User"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Session already exists");

        verify(sessionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroupSession - throws EntityNotFoundException if appointment not found")
    void createGroupSessionThrowsIfAppointmentNotFound() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());
        final List<UUID> clientIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // When / Then
        assertThatThrownBy(() -> service.createGroupSession(
                appointmentId, clientIds, SessionStatus.PENDING, actorUserId, "Test User"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    @DisplayName("createGroupSession - exactly 20 participants is accepted")
    void createGroupSessionAcceptsExactlyCap() {
        // Given — exactly 20 participants (boundary test)
        final List<UUID> clientIds =
                java.util.stream.Stream.generate(UUID::randomUUID).limit(20).toList();

        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(participantRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(auditRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        final SessionRecord result = service.createGroupSession(
                appointmentId, clientIds, SessionStatus.PENDING, actorUserId, "Test User");

        // Then — should not throw and should save 20 participants
        assertThat(result.getRecordKind()).isEqualTo(RecordKind.GROUP);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<SessionParticipant>> captor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(20);
    }

    @Test
    @DisplayName("createGroupSession - audit entries have actorUserId and actorName set")
    void createGroupSessionAuditEntriesHaveActorInfo() {
        // Given
        final List<UUID> clientIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        final String actorName = "Dr. Smith";

        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(participantRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(auditRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // When
        service.createGroupSession(
                appointmentId, clientIds, SessionStatus.PENDING, actorUserId, actorName);

        // Then
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<SessionParticipantAudit>> auditCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(auditRepository).saveAll(auditCaptor.capture());

        final List<SessionParticipantAudit> entries = auditCaptor.getValue();
        assertThat(entries).allMatch(e -> actorUserId.equals(e.getActorUserId()));
        assertThat(entries).allMatch(e -> actorName.equals(e.getActorName()));
    }
}
