package com.psyassistant.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.AuditActionType;
import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AppointmentService}.
 *
 * <p>Tests business logic for:
 * <ul>
 *     <li>Appointment creation with conflict detection</li>
 *     <li>Conflict override handling</li>
 *     <li>Appointment rescheduling with conflict detection</li>
 *     <li>Appointment cancellation with validation</li>
 *     <li>Input validation (duration, null checks)</li>
 *     <li>Audit logging integration</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Appointment Service Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SessionTypeRepository sessionTypeRepository;

    @Mock
    private ConflictDetectionService conflictDetectionService;

    @Mock
    private AppointmentAuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID therapistId;
    private UUID clientId;
    private UUID sessionTypeId;
    private UUID actorUserId;
    private SessionType sessionType;
    private ZonedDateTime startTime;

    @BeforeEach
    void setUp() {
        therapistId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        sessionTypeId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        startTime = ZonedDateTime.of(2026, 3, 31, 10, 0, 0, 0, ZoneId.of("America/New_York"));

        sessionType = new SessionType("IN_PERSON", "In-Person Session", null);
        ReflectionTestUtils.setField(sessionType, "id", sessionTypeId);
    }

    // ========== Create Appointment Tests ==========

    @Test
    @DisplayName("Create appointment successfully with no conflicts")
    void testCreateAppointmentNoConflictsSuccess() {
        // Arrange
        when(sessionTypeRepository.findById(sessionTypeId)).thenReturn(Optional.of(sessionType));
        when(conflictDetectionService.findConflictingAppointments(therapistId, startTime, 60))
                .thenReturn(Collections.emptyList());

        final Appointment savedAppointment = createMockAppointment();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        // Act
        final Appointment result = appointmentService.createAppointment(
                therapistId,
                clientId,
                sessionTypeId,
                startTime,
                60,
                "America/New_York",
                "Initial consultation",
                false,
                actorUserId,
                "Dr. Smith"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(result.getIsConflictOverride()).isFalse();

        verify(sessionTypeRepository).findById(sessionTypeId);
        verify(conflictDetectionService).findConflictingAppointments(
                therapistId, startTime, 60);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(auditService).recordAuditEntry(
                any(UUID.class), eq(AuditActionType.CREATED), eq(actorUserId), eq("Dr. Smith"));
    }

    @Test
    @DisplayName("Create appointment with conflict throws ConflictException when override not allowed")
    void testCreateAppointmentConflictWithoutOverrideThrowsException() {
        // Arrange
        when(sessionTypeRepository.findById(sessionTypeId)).thenReturn(Optional.of(sessionType));

        final Appointment conflictingAppointment = createMockAppointment();
        when(conflictDetectionService.findConflictingAppointments(therapistId, startTime, 60))
                .thenReturn(List.of(conflictingAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                therapistId,
                clientId,
                sessionTypeId,
                startTime,
                60,
                "America/New_York",
                "Initial consultation",
                false, // Override NOT allowed
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(AppointmentService.ConflictException.class)
                .hasMessageContaining("Appointment conflicts with existing bookings")
                .extracting(ex -> ((AppointmentService.ConflictException) ex).getConflictingAppointments())
                .asList()
                .hasSize(1);

        verify(appointmentRepository, never()).save(any());
        verify(auditService, never()).recordAuditEntry(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Create appointment with conflict override creates appointment with override flag")
    void testCreateAppointmentConflictWithOverrideSuccess() {
        // Arrange
        when(sessionTypeRepository.findById(sessionTypeId)).thenReturn(Optional.of(sessionType));

        final Appointment conflictingAppointment = createMockAppointment();
        when(conflictDetectionService.findConflictingAppointments(therapistId, startTime, 60))
                .thenReturn(List.of(conflictingAppointment));

        final Appointment savedAppointment = createMockAppointment();
        savedAppointment.setIsConflictOverride(true);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        // Act
        final Appointment result = appointmentService.createAppointment(
                therapistId,
                clientId,
                sessionTypeId,
                startTime,
                60,
                "America/New_York",
                "Emergency appointment",
                true, // Override allowed
                actorUserId,
                "Dr. Smith"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsConflictOverride()).isTrue();

        verify(appointmentRepository).save(any(Appointment.class));
        verify(auditService).recordAuditEntryWithMetadata(
                any(UUID.class),
                eq(AuditActionType.CONFLICT_OVERRIDE),
                eq(actorUserId),
                eq("Dr. Smith"),
                anyString()
        );
    }

    @Test
    @DisplayName("Create appointment with invalid duration throws IllegalArgumentException")
    void testCreateAppointmentInvalidDurationThrowsException() {
        // Act & Assert - Duration not multiple of 15
        assertThatThrownBy(() -> appointmentService.createAppointment(
                therapistId,
                clientId,
                sessionTypeId,
                startTime,
                35, // Invalid: not multiple of 15
                "America/New_York",
                null,
                false,
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be multiple of 15 minutes");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create appointment with duration exceeding 8 hours throws IllegalArgumentException")
    void testCreateAppointmentDurationTooLongThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                therapistId,
                clientId,
                sessionTypeId,
                startTime,
                500, // Invalid: exceeds 480 minutes (8 hours)
                "America/New_York",
                null,
                false,
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration cannot exceed 8 hours");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create appointment with non-existent session type throws EntityNotFoundException")
    void testCreateAppointmentSessionTypeNotFoundThrowsException() {
        // Arrange
        when(sessionTypeRepository.findById(sessionTypeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                therapistId,
                clientId,
                sessionTypeId,
                startTime,
                60,
                "America/New_York",
                null,
                false,
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Session type not found");

        verify(appointmentRepository, never()).save(any());
    }

    // ========== Reschedule Appointment Tests ==========

    @Test
    @DisplayName("Reschedule appointment successfully with no conflicts")
    void testRescheduleAppointmentNoConflictsSuccess() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        final Appointment existingAppointment = createMockAppointment();
        ReflectionTestUtils.setField(existingAppointment, "id", appointmentId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));

        final ZonedDateTime newStartTime = startTime.plusDays(1); // Reschedule to next day
        when(conflictDetectionService.findConflictingAppointmentsExcluding(
                therapistId, newStartTime, 60, appointmentId))
                .thenReturn(Collections.emptyList());

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        // Act
        final Appointment result = appointmentService.rescheduleAppointment(
                appointmentId,
                newStartTime,
                "Client requested new time",
                false,
                actorUserId,
                "Dr. Smith"
        );

        // Assert
        assertThat(result).isNotNull();
        verify(conflictDetectionService).findConflictingAppointmentsExcluding(
                therapistId, newStartTime, 60, appointmentId);
        verify(appointmentRepository).save(existingAppointment);
        verify(auditService).recordAuditEntryWithMetadata(
                eq(appointmentId),
                eq(AuditActionType.RESCHEDULED),
                eq(actorUserId),
                eq("Dr. Smith"),
                anyString()
        );
    }

    @Test
    @DisplayName("Reschedule appointment with conflict throws ConflictException when override not allowed")
    void testRescheduleAppointmentConflictWithoutOverrideThrowsException() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        final Appointment existingAppointment = createMockAppointment();
        ReflectionTestUtils.setField(existingAppointment, "id", appointmentId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));

        final ZonedDateTime newStartTime = startTime.plusHours(2);
        final Appointment conflictingAppointment = createMockAppointment();
        when(conflictDetectionService.findConflictingAppointmentsExcluding(
                therapistId, newStartTime, 60, appointmentId))
                .thenReturn(List.of(conflictingAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(
                appointmentId,
                newStartTime,
                "Client requested new time",
                false, // Override not allowed
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(AppointmentService.ConflictException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reschedule cancelled appointment throws IllegalStateException")
    void testRescheduleAppointmentAlreadyCancelledThrowsException() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        final Appointment cancelledAppointment = createMockAppointment();
        ReflectionTestUtils.setField(cancelledAppointment, "id", appointmentId);
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(cancelledAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(
                appointmentId,
                startTime.plusDays(1),
                "Trying to reschedule cancelled appointment",
                false,
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reschedule cancelled appointment");

        verify(conflictDetectionService, never()).findConflictingAppointmentsExcluding(any(), any(), any(), any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reschedule non-existent appointment throws EntityNotFoundException")
    void testRescheduleAppointmentNotFoundThrowsException() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(
                appointmentId,
                startTime.plusDays(1),
                "Reason",
                false,
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    // ========== Cancel Appointment Tests ==========

    @Test
    @DisplayName("Cancel appointment successfully")
    void testCancelAppointmentSuccess() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        final Appointment existingAppointment = createMockAppointment();
        ReflectionTestUtils.setField(existingAppointment, "id", appointmentId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingAppointment);

        // Act
        final Appointment result = appointmentService.cancelAppointment(
                appointmentId,
                CancellationType.CLIENT_INITIATED,
                "Client has a conflicting work meeting",
                actorUserId,
                "Dr. Smith"
        );

        // Assert
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(existingAppointment);
        verify(auditService).recordAuditEntryWithMetadata(
                eq(appointmentId),
                eq(AuditActionType.CANCELLED),
                eq(actorUserId),
                eq("Dr. Smith"),
                anyString()
        );
    }

    @Test
    @DisplayName("Cancel already cancelled appointment throws IllegalStateException")
    void testCancelAppointmentAlreadyCancelledThrowsException() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        final Appointment cancelledAppointment = createMockAppointment();
        ReflectionTestUtils.setField(cancelledAppointment, "id", appointmentId);
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(cancelledAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancelAppointment(
                appointmentId,
                CancellationType.CLIENT_INITIATED,
                "Trying to cancel already cancelled appointment",
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Appointment is already cancelled");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel non-existent appointment throws EntityNotFoundException")
    void testCancelAppointmentNotFoundThrowsException() {
        // Arrange
        final UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancelAppointment(
                appointmentId,
                CancellationType.THERAPIST_INITIATED,
                "Reason",
                actorUserId,
                "Dr. Smith"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    // ========== Helper Methods ==========

    private Appointment createMockAppointment() {
        final Appointment appointment = new Appointment(
                therapistId,
                clientId,
                sessionType,
                startTime,
                60,
                "America/New_York"
        );
        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setIsConflictOverride(false);
        return appointment;
    }
}
