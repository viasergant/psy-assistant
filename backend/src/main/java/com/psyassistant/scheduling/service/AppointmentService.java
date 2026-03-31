package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.AuditActionType;
import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing appointment lifecycle operations.
 *
 * <p>Provides:
 * <ul>
 *     <li>Appointment creation with conflict detection and override support</li>
 *     <li>Appointment rescheduling with reason tracking</li>
 *     <li>Appointment cancellation with type classification</li>
 *     <li>Optimistic locking retry logic for concurrent modifications</li>
 *     <li>Immutable audit trail for all operations</li>
 * </ul>
 *
 * <p><strong>Concurrency Handling</strong>: Uses {@code @Retryable} to automatically retry
 * on optimistic locking failures (max 3 attempts, 50ms backoff with 2x multiplier).
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final AppointmentAuditService auditService;

    public AppointmentService(final AppointmentRepository appointmentRepository,
                               final SessionTypeRepository sessionTypeRepository,
                               final ConflictDetectionService conflictDetectionService,
                               final AppointmentAuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.sessionTypeRepository = sessionTypeRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.auditService = auditService;
    }

    /**
     * Creates a new appointment with automatic conflict detection.
     *
     * <p><strong>Conflict Handling</strong>:
     * <ul>
     *     <li>If conflicts detected and {@code allowConflictOverride = false}: throws {@link ConflictException}</li>
     *     <li>If conflicts detected and {@code allowConflictOverride = true}: creates appointment with override flag</li>
     *     <li>If no conflicts: creates appointment normally</li>
     * </ul>
     *
     * <p><strong>Optimistic Locking Retry</strong>: Automatically retries up to 3 times on concurrent modification.
     * Retry backoff: 50ms, 100ms, 200ms (exponential 2x multiplier).
     *
     * @param therapistProfileId therapist UUID
     * @param clientId client UUID
     * @param sessionTypeId session type UUID
     * @param startTime appointment start time
     * @param durationMinutes duration in minutes (must be multiple of 15)
     * @param timezone IANA timezone identifier
     * @param notes optional appointment notes
     * @param allowConflictOverride if true, create appointment even if conflicts exist
     * @param actorUserId user creating the appointment (for audit)
     * @param actorName display name of actor (for audit)
     * @return created appointment
     * @throws ConflictException if conflicts exist and override not allowed
     * @throws EntityNotFoundException if session type not found
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public Appointment createAppointment(final UUID therapistProfileId,
                                          final UUID clientId,
                                          final UUID sessionTypeId,
                                          final ZonedDateTime startTime,
                                          final Integer durationMinutes,
                                          final String timezone,
                                          final String notes,
                                          final boolean allowConflictOverride,
                                          final UUID actorUserId,
                                          final String actorName) {

        // Validate inputs
        validateDuration(durationMinutes);

        // Fetch session type
        final SessionType sessionType = sessionTypeRepository.findById(sessionTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Session type not found: " + sessionTypeId));

        // Conflict detection
        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                therapistProfileId,
                startTime,
                durationMinutes
        );

        if (!conflicts.isEmpty() && !allowConflictOverride) {
            log.warn("Appointment conflict detected: therapist={}, startTime={}, duration={}, conflictCount={}",
                    therapistProfileId, startTime, durationMinutes, conflicts.size());
            throw new ConflictException("Appointment conflicts with existing bookings", conflicts);
        }

        // Create appointment
        final Appointment appointment = new Appointment(
                therapistProfileId,
                clientId,
                sessionType,
                startTime,
                durationMinutes,
                timezone
        );

        appointment.setNotes(notes);

        if (!conflicts.isEmpty() && allowConflictOverride) {
            appointment.setIsConflictOverride(true);
            log.info("Appointment created with conflict override: therapist={}, startTime={}, conflictCount={}",
                    therapistProfileId, startTime, conflicts.size());
        }

        final Appointment saved = appointmentRepository.save(appointment);

        // Async audit trail
        if (saved.getIsConflictOverride()) {
            // Include conflict details in audit metadata
            final String metadata = String.format(
                    "{\"conflictCount\": %d, \"conflictingAppointmentIds\": [%s]}",
                    conflicts.size(),
                    conflicts.stream()
                            .map(c -> "\"" + c.getId() + "\"")
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")
            );
            auditService.recordAuditEntryWithMetadata(
                    saved.getId(),
                    AuditActionType.CONFLICT_OVERRIDE,
                    actorUserId,
                    actorName,
                    metadata
            );
        } else {
            auditService.recordAuditEntry(
                    saved.getId(),
                    AuditActionType.CREATED,
                    actorUserId,
                    actorName
            );
        }

        log.info("Appointment created: id={}, therapist={}, client={}, startTime={}",
                saved.getId(), therapistProfileId, clientId, startTime);

        return saved;
    }

    /**
     * Retrieves an appointment by ID.
     *
     * @param appointmentId appointment UUID
     * @return appointment
     * @throws EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Appointment getAppointment(final UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found: " + appointmentId));
    }

    /**
     * Finds all appointments for a therapist within a date range.
     *
     * @param therapistProfileId therapist UUID
     * @param startDate start of range (inclusive)
     * @param endDate end of range (exclusive)
     * @return list of appointments
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByTherapistAndDateRange(final UUID therapistProfileId,
                                                                      final ZonedDateTime startDate,
                                                                      final ZonedDateTime endDate) {
        return appointmentRepository.findByTherapistAndDateRange(therapistProfileId, startDate, endDate);
    }

    /**
     * Finds all appointments for a client.
     *
     * @param clientId client UUID
     * @return list of appointments (most recent first)
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByClient(final UUID clientId) {
        return appointmentRepository.findByClientIdOrderByStartTimeDesc(clientId);
    }

    // ========== Validation Helpers ==========

    private void validateDuration(final Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive: " + durationMinutes);
        }
        if (durationMinutes % 15 != 0) {
            throw new IllegalArgumentException("Duration must be multiple of 15 minutes: " + durationMinutes);
        }
        if (durationMinutes > 480) {
            throw new IllegalArgumentException("Duration cannot exceed 8 hours: " + durationMinutes);
        }
    }

    // ========== Custom Exceptions ==========

    /**
     * Exception thrown when an appointment conflicts with existing bookings.
     */
    public static class ConflictException extends RuntimeException {
        private final List<Appointment> conflictingAppointments;

        public ConflictException(final String message, final List<Appointment> conflictingAppointments) {
            super(message);
            this.conflictingAppointments = conflictingAppointments;
        }

        public List<Appointment> getConflictingAppointments() {
            return conflictingAppointments;
        }
    }
}
