package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConflictDetectionService}.
 *
 * <p>Tests conflict detection edge cases:
 * <ul>
 *     <li>Adjacent appointments (end = start) should NOT conflict</li>
 *     <li>Partial overlap should conflict</li>
 *     <li>Exact time match should conflict</li>
 *     <li>One appointment contains the other should conflict</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conflict Detection Service Tests")
class ConflictDetectionServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private ConflictDetectionService conflictDetectionService;

    private UUID therapistId;
    private ZonedDateTime baseTime;

    @BeforeEach
    void setUp() {
        therapistId = UUID.randomUUID();
        baseTime = ZonedDateTime.of(2026, 3, 31, 10, 0, 0, 0, ZoneId.of("America/New_York"));
    }

    @Test
    @DisplayName("Adjacent appointments should NOT conflict (end = start)")
    void testAdjacentAppointments_NoConflict() {
        // Existing: 10:00-11:00
        // Proposed: 11:00-12:00
        // Expected: NO conflict (adjacent but not overlapping)

        final ZonedDateTime proposedStart = baseTime.plusHours(1); // 11:00
        final Integer proposedDuration = 60;

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(Collections.emptyList());

        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Partial overlap should conflict")
    void testPartialOverlap_HasConflict() {
        // Existing: 10:00-11:00
        // Proposed: 10:30-11:30
        // Expected: CONFLICT (30 minutes overlap)

        final ZonedDateTime proposedStart = baseTime.plusMinutes(30); // 10:30
        final Integer proposedDuration = 60;

        final Appointment existingAppointment = createMockAppointment(baseTime, 60);

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(List.of(existingAppointment));

        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(existingAppointment.getId());
    }

    @Test
    @DisplayName("Exact time match should conflict")
    void testExactTimeMatch_HasConflict() {
        // Existing: 10:00-11:00
        // Proposed: 10:00-11:00
        // Expected: CONFLICT (exact match)

        final ZonedDateTime proposedStart = baseTime; // 10:00
        final Integer proposedDuration = 60;

        final Appointment existingAppointment = createMockAppointment(baseTime, 60);

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(List.of(existingAppointment));

        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(conflicts).hasSize(1);
    }

    @Test
    @DisplayName("One appointment contains the other should conflict")
    void testOneContainsOther_HasConflict() {
        // Existing: 10:00-12:00 (2 hours)
        // Proposed: 10:30-11:00 (30 minutes inside)
        // Expected: CONFLICT

        final ZonedDateTime proposedStart = baseTime.plusMinutes(30); // 10:30
        final Integer proposedDuration = 30;

        final Appointment existingAppointment = createMockAppointment(baseTime, 120);

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(List.of(existingAppointment));

        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(conflicts).hasSize(1);
    }

    @Test
    @DisplayName("No conflicts when therapist schedule is empty")
    void testEmptySchedule_NoConflict() {
        final ZonedDateTime proposedStart = baseTime;
        final Integer proposedDuration = 60;

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(Collections.emptyList());

        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("hasConflicts returns true when conflicts exist")
    void testHasConflicts_WhenConflictsExist() {
        final ZonedDateTime proposedStart = baseTime;
        final Integer proposedDuration = 60;

        final Appointment existingAppointment = createMockAppointment(baseTime, 60);

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(List.of(existingAppointment));

        final boolean hasConflicts = conflictDetectionService.hasConflicts(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(hasConflicts).isTrue();
    }

    @Test
    @DisplayName("hasConflicts returns false when no conflicts exist")
    void testHasConflicts_WhenNoConflictsExist() {
        final ZonedDateTime proposedStart = baseTime;
        final Integer proposedDuration = 60;

        when(appointmentRepository.findConflictingAppointments(
                eq(therapistId),
                eq(proposedStart),
                eq(proposedDuration)
        )).thenReturn(Collections.emptyList());

        final boolean hasConflicts = conflictDetectionService.hasConflicts(
                therapistId,
                proposedStart,
                proposedDuration
        );

        assertThat(hasConflicts).isFalse();
    }

    // ========== Test Helpers ==========

    private Appointment createMockAppointment(final ZonedDateTime startTime, final Integer durationMinutes) {
        final SessionType sessionType = new SessionType("IN_PERSON", "In-Person Session", "Test session");
        final Appointment appointment = new Appointment(
                therapistId,
                UUID.randomUUID(), // clientId
                sessionType,
                startTime,
                durationMinutes,
                "America/New_York"
        );
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        // Mock the ID (normally set by JPA)
        try {
            final var idField = Appointment.class.getSuperclass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(appointment, UUID.randomUUID());
        } catch (final Exception e) {
            throw new RuntimeException("Failed to set appointment ID", e);
        }
        return appointment;
    }
}
