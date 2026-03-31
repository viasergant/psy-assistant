package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for detecting appointment conflicts using PostgreSQL tstzrange queries.
 *
 * <p>Provides fast O(log n) conflict detection via GIST-indexed overlap queries.
 * Performance target: < 500ms for therapist schedules with 500+ appointments.
 *
 * <p><strong>Technical Details</strong>:
 * <ul>
 *     <li>Uses native PostgreSQL tstzrange with GIST index</li>
 *     <li>Overlap operator ({@code &&}) leverages index for fast lookups</li>
 *     <li>Partial index excludes CANCELLED appointments automatically</li>
 * </ul>
 */
@Service
public class ConflictDetectionService {

    private final AppointmentRepository appointmentRepository;

    public ConflictDetectionService(final AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Finds all existing appointments that overlap with the proposed time slot.
     *
     * <p><strong>Overlap definition</strong>: Two appointments overlap if their time ranges intersect:
     * <pre>
     * Existing:  [10:00 -------- 11:00]
     * Proposed:        [10:30 -------- 11:30]  ← Overlaps!
     * </pre>
     *
     * <p><strong>Edge cases handled correctly</strong>:
     * <ul>
     *     <li>Adjacent appointments (end = start): <strong>NOT</strong> a conflict</li>
     *     <li>Exact time match: conflict</li>
     *     <li>Partial overlap: conflict</li>
     *     <li>One contains the other: conflict</li>
     * </ul>
     *
     * @param therapistProfileId therapist UUID
     * @param startTime proposed appointment start time
     * @param durationMinutes proposed duration in minutes
     * @return list of conflicting appointments (empty if no conflicts)
     */
    @Transactional(readOnly = true)
    public List<Appointment> findConflictingAppointments(final UUID therapistProfileId,
                                                           final ZonedDateTime startTime,
                                                           final Integer durationMinutes) {
        return appointmentRepository.findConflictingAppointments(
                therapistProfileId,
                startTime,
                durationMinutes
        );
    }

    /**
     * Checks for conflicts excluding a specific appointment (used during reschedule).
     *
     * @param therapistProfileId therapist UUID
     * @param startTime proposed new start time
     * @param durationMinutes proposed duration
     * @param excludeAppointmentId appointment ID to exclude from conflict check
     * @return list of conflicting appointments
     */
    @Transactional(readOnly = true)
    public List<Appointment> findConflictingAppointmentsExcluding(final UUID therapistProfileId,
                                                                    final ZonedDateTime startTime,
                                                                    final Integer durationMinutes,
                                                                    final UUID excludeAppointmentId) {
        return appointmentRepository.findConflictingAppointmentsExcluding(
                therapistProfileId,
                startTime,
                durationMinutes,
                excludeAppointmentId
        );
    }

    /**
     * Checks if a proposed time slot has any conflicts.
     *
     * @param therapistProfileId therapist UUID
     * @param startTime proposed start time
     * @param durationMinutes proposed duration
     * @return true if conflicts exist, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasConflicts(final UUID therapistProfileId,
                                 final ZonedDateTime startTime,
                                 final Integer durationMinutes) {
        return !findConflictingAppointments(therapistProfileId, startTime, durationMinutes).isEmpty();
    }
}
