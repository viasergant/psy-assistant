package com.psyassistant.sessions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.event.AppointmentStatusChangedEvent;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
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
 * Unit tests for {@link SessionRecordService}.
 */
@ExtendWith(MockitoExtension.class)
class SessionRecordServiceTest {

    @Mock
    private SessionRecordRepository sessionRecordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SessionRecordService sessionRecordService;

    private UUID appointmentId;
    private UUID clientId;
    private UUID therapistId;
    private UUID actorUserId;
    private Appointment appointment;
    private SessionType sessionType;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        therapistId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        sessionType = new SessionType("ONLINE", "Online Session", "Virtual session via video call");

        appointment = new Appointment(
                therapistId,
                clientId,
                sessionType,
                ZonedDateTime.now().plusDays(1),
                60,
                "America/New_York"
        );
    }

    @Test
    @DisplayName("handleAppointmentStatusChanged - creates session when status changes to COMPLETED")
    void handleAppointmentStatusChangedCreatesSessionForCompleted() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));

        final AppointmentStatusChangedEvent event = AppointmentStatusChangedEvent.of(
                appointmentId,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.COMPLETED,
                actorUserId,
                "Test User"
        );

        // When
        sessionRecordService.handleAppointmentStatusChanged(event);

        // Then
        final ArgumentCaptor<SessionRecord> captor = ArgumentCaptor.forClass(SessionRecord.class);
        verify(sessionRecordRepository).save(captor.capture());

        final SessionRecord savedSession = captor.getValue();
        assertThat(savedSession.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(savedSession.getClientId()).isEqualTo(clientId);
        assertThat(savedSession.getTherapistId()).isEqualTo(therapistId);
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.PENDING);
    }

    @Test
    @DisplayName("handleAppointmentStatusChanged - creates session when status changes to IN_PROGRESS")
    void handleAppointmentStatusChangedCreatesSessionForInProgress() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));

        final AppointmentStatusChangedEvent event = AppointmentStatusChangedEvent.of(
                appointmentId,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.IN_PROGRESS,
                actorUserId,
                "Test User"
        );

        // When
        sessionRecordService.handleAppointmentStatusChanged(event);

        // Then
        final ArgumentCaptor<SessionRecord> captor = ArgumentCaptor.forClass(SessionRecord.class);
        verify(sessionRecordRepository).save(captor.capture());

        final SessionRecord savedSession = captor.getValue();
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("handleAppointmentStatusChanged - skips session creation if already exists")
    void handleAppointmentStatusChangedSkipsIfDuplicate() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

        final AppointmentStatusChangedEvent event = AppointmentStatusChangedEvent.of(
                appointmentId,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.COMPLETED,
                actorUserId,
                "Test User"
        );

        // When
        sessionRecordService.handleAppointmentStatusChanged(event);

        // Then
        verify(sessionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleAppointmentStatusChanged - cancels existing session when appointment cancelled")
    void handleAppointmentStatusChangedCancelsSessionWhenAppointmentCancelled() {
        // Given
        final SessionRecord existingSession = new SessionRecord(
                appointmentId,
                clientId,
                therapistId,
                appointment.getStartTime().toLocalDate(),
                appointment.getStartTime().toLocalTime(),
                sessionType,
                java.time.Duration.ofMinutes(60),
                SessionStatus.IN_PROGRESS
        );

        when(sessionRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(existingSession));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));

        final AppointmentStatusChangedEvent event = AppointmentStatusChangedEvent.of(
                appointmentId,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.CANCELLED,
                actorUserId,
                "Test User"
        );

        // When
        sessionRecordService.handleAppointmentStatusChanged(event);

        // Then
        final ArgumentCaptor<SessionRecord> captor = ArgumentCaptor.forClass(SessionRecord.class);
        verify(sessionRecordRepository).save(captor.capture());

        final SessionRecord savedSession = captor.getValue();
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(savedSession.getCancellationReason()).isEqualTo("Appointment was cancelled");
    }

    @Test
    @DisplayName("startSession - creates session and updates appointment to IN_PROGRESS")
    void startSessionSuccess() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(sessionRecordRepository.save(any(SessionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        // When
        final SessionRecord result = sessionRecordService.startSession(
                appointmentId,
                actorUserId,
                "Test User"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(result.getAppointmentId()).isEqualTo(appointmentId);

        verify(sessionRecordRepository).save(any(SessionRecord.class));
        verify(appointmentRepository).save(any(Appointment.class));
        verify(eventPublisher).publishEvent(any(AppointmentStatusChangedEvent.class));
    }

    @Test
    @DisplayName("startSession - throws IllegalStateException if session already exists")
    void startSessionThrowsIfDuplicate() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> sessionRecordService.startSession(
                appointmentId,
                actorUserId,
                "Test User"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Session already exists");

        verify(sessionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSession - throws EntityNotFoundException if appointment not found")
    void startSessionThrowsIfAppointmentNotFound() {
        // Given
        when(sessionRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> sessionRecordService.startSession(
                appointmentId,
                actorUserId,
                "Test User"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    @DisplayName("getSessionRecord - retrieves existing session")
    void getSessionRecordSuccess() {
        // Given
        final UUID sessionId = UUID.randomUUID();
        final SessionRecord session = new SessionRecord(
                appointmentId,
                clientId,
                therapistId,
                appointment.getStartTime().toLocalDate(),
                appointment.getStartTime().toLocalTime(),
                sessionType,
                java.time.Duration.ofMinutes(60),
                SessionStatus.PENDING
        );

        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // When
        final SessionRecord result = sessionRecordService.getSessionRecord(sessionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(session);
    }

    @Test
    @DisplayName("getSessionRecord - throws EntityNotFoundException if not found")
    void getSessionRecordThrowsIfNotFound() {
        // Given
        final UUID sessionId = UUID.randomUUID();
        when(sessionRecordRepository.findById(sessionId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> sessionRecordService.getSessionRecord(sessionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    @DisplayName("getSessionByAppointmentId - retrieves session by appointment ID")
    void getSessionByAppointmentIdSuccess() {
        // Given
        final SessionRecord session = new SessionRecord(
                appointmentId,
                clientId,
                therapistId,
                appointment.getStartTime().toLocalDate(),
                appointment.getStartTime().toLocalTime(),
                sessionType,
                java.time.Duration.ofMinutes(60),
                SessionStatus.PENDING
        );

        when(sessionRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.of(session));

        // When
        final SessionRecord result = sessionRecordService.getSessionByAppointmentId(appointmentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(session);
    }

    @Test
    @DisplayName("getSessionByAppointmentId - throws EntityNotFoundException if not found")
    void getSessionByAppointmentIdThrowsIfNotFound() {
        // Given
        when(sessionRecordRepository.findByAppointmentId(appointmentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> sessionRecordService.getSessionByAppointmentId(appointmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No session found for appointment");
    }
}
