package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.AuditActionType;
import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.event.AppointmentStatusChangedEvent;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final AppointmentAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(final AppointmentRepository appointmentRepository,
                               final SessionTypeRepository sessionTypeRepository,
                               final ConflictDetectionService conflictDetectionService,
                               final AppointmentAuditService auditService,
                               final ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.sessionTypeRepository = sessionTypeRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new appointment with automatic conflict detection.
     *
     * <p><strong>Conflict Handling</strong>:
     * <ul>
     *     <li>If conflicts detected and {@code allowConflictOverride = false}:
     *         throws {@link ConflictException}</li>
     *     <li>If conflicts detected and {@code allowConflictOverride = true}:
     *         creates appointment with override flag</li>
     *     <li>If no conflicts: creates appointment normally</li>
     * </ul>
     *
     * <p><strong>Optimistic Locking Retry</strong>: Automatically retries up to 3 times
     * on concurrent modification. Retry backoff: 50ms, 100ms, 200ms (exponential 2x multiplier).
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
            LOG.warn("Appointment conflict detected: therapist={}, startTime={}, duration={}, conflictCount={}",
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
            LOG.info("Appointment created with conflict override: therapist={}, startTime={}, conflictCount={}",
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

        LOG.info("Appointment created: id={}, therapist={}, client={}, startTime={}",
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

    /**
     * Reschedules an existing appointment to a new time.
     *
     * <p>Applies conflict detection to the new time slot (excluding the appointment being rescheduled).
     * Preserves the original start time and tracks reschedule metadata.
     *
     * <p><strong>Optimistic Locking Retry</strong>: Automatically retries up to 3 times on concurrent modification.
     *
     * @param appointmentId appointment UUID to reschedule
     * @param newStartTime new start time for the appointment
     * @param reason human-readable reason for reschedule (min 10 chars)
     * @param allowConflictOverride if true, reschedule even if conflicts exist at new time
     * @param actorUserId user performing the reschedule (for audit)
     * @param actorName display name of actor (for audit)
     * @return rescheduled appointment
     * @throws EntityNotFoundException if appointment not found
     * @throws ConflictException if new time conflicts and override not allowed
     * @throws IllegalStateException if appointment is already cancelled
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public Appointment rescheduleAppointment(final UUID appointmentId,
                                              final ZonedDateTime newStartTime,
                                              final String reason,
                                              final boolean allowConflictOverride,
                                              final UUID actorUserId,
                                              final String actorName) {

        // Fetch existing appointment
        final Appointment appointment = getAppointment(appointmentId);

        // Validate appointment is not cancelled
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reschedule cancelled appointment: " + appointmentId);
        }

        // Conflict detection for new time (exclude current appointment)
        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointmentsExcluding(
                appointment.getTherapistProfileId(),
                newStartTime,
                appointment.getDurationMinutes(),
                appointmentId
        );

        if (!conflicts.isEmpty() && !allowConflictOverride) {
            LOG.warn("Reschedule conflict detected: appointmentId={}, newStartTime={}, conflictCount={}",
                    appointmentId, newStartTime, conflicts.size());
            throw new ConflictException("New time slot conflicts with existing bookings", conflicts);
        }

        // Reschedule via entity method
        appointment.reschedule(newStartTime, reason, actorUserId);

        if (!conflicts.isEmpty() && allowConflictOverride) {
            appointment.setIsConflictOverride(true);
            LOG.info("Appointment rescheduled with conflict override: id={}, newStartTime={}",
                    appointmentId, newStartTime);
        }

        final Appointment saved = appointmentRepository.save(appointment);

        // Async audit trail
        final ZonedDateTime oldTime = appointment.getOriginalStartTime() != null
                ? appointment.getOriginalStartTime()
                : appointment.getStartTime();
        final String metadata = String.format(
                "{\"oldStartTime\": \"%s\", \"newStartTime\": \"%s\", \"reason\": \"%s\"}",
                oldTime,
                newStartTime,
                reason.replace("\"", "\\\"")
        );

        if (!conflicts.isEmpty() && allowConflictOverride) {
            auditService.recordAuditEntryWithMetadata(
                    saved.getId(),
                    AuditActionType.CONFLICT_OVERRIDE,
                    actorUserId,
                    actorName,
                    metadata
            );
        }

        auditService.recordAuditEntryWithMetadata(
                saved.getId(),
                AuditActionType.RESCHEDULED,
                actorUserId,
                actorName,
                metadata
        );

        LOG.info("Appointment rescheduled: id={}, oldStartTime={}, newStartTime={}",
                saved.getId(), appointment.getStartTime(), newStartTime);

        return saved;
    }

    /**
     * Cancels an existing appointment with classification and reason.
     *
     * <p>Once cancelled, the time slot becomes available for new bookings.
     * Records full cancellation metadata including type, reason, timestamp, and cancelling user.
     *
     * <p><strong>Optimistic Locking Retry</strong>: Automatically retries up to 3 times on concurrent modification.
     *
     * @param appointmentId appointment UUID to cancel
     * @param cancellationType who initiated cancellation (CLIENT_INITIATED, THERAPIST_INITIATED, LATE_CANCELLATION)
     * @param reason human-readable cancellation reason (min 10 chars)
     * @param actorUserId user performing the cancellation (for audit)
     * @param actorName display name of actor (for audit)
     * @return cancelled appointment
     * @throws EntityNotFoundException if appointment not found
     * @throws IllegalStateException if appointment is already cancelled
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public Appointment cancelAppointment(final UUID appointmentId,
                                          final CancellationType cancellationType,
                                          final String reason,
                                          final UUID actorUserId,
                                          final String actorName) {

        // Fetch existing appointment
        final Appointment appointment = getAppointment(appointmentId);

        // Validate appointment is not already cancelled
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment is already cancelled: " + appointmentId);
        }

        // Capture old status before change
        final AppointmentStatus oldStatus = appointment.getStatus();

        // Cancel via entity method
        appointment.cancel(cancellationType, reason, actorUserId);

        final Appointment saved = appointmentRepository.save(appointment);

        // Async audit trail
        final String metadata = String.format(
                "{\"cancellationType\": \"%s\", \"reason\": \"%s\", \"originalStartTime\": \"%s\"}",
                cancellationType,
                reason.replace("\"", "\\\""),
                appointment.getStartTime()
        );

        auditService.recordAuditEntryWithMetadata(
                saved.getId(),
                AuditActionType.CANCELLED,
                actorUserId,
                actorName,
                metadata
        );

        // Publish event for session record creation/cancellation
        eventPublisher.publishEvent(AppointmentStatusChangedEvent.of(
                saved.getId(),
                oldStatus,
                AppointmentStatus.CANCELLED,
                actorUserId,
                actorName
        ));

        LOG.info("Appointment cancelled: id={}, type={}, reason={}, oldStatus={}",
                saved.getId(), cancellationType, reason, oldStatus);

        return saved;
    }

    /**
     * Updates an appointment's status.
     *
     * <p>Allows manual status transitions (e.g., marking as CONFIRMED, COMPLETED, or NO_SHOW).
     * Publishes status change event for potential session record updates.
     *
     * <p><strong>Optimistic Locking Retry</strong>: Automatically retries up to 3 times on concurrent modification.
     *
     * @param appointmentId appointment UUID
     * @param newStatus new appointment status
     * @param notes optional notes about the status change
     * @param actorUserId user performing the update (for audit)
     * @param actorName display name of actor (for audit)
     * @return updated appointment
     * @throws EntityNotFoundException if appointment not found
     * @throws IllegalArgumentException if invalid status transition
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public Appointment updateAppointmentStatus(final UUID appointmentId,
                                                final AppointmentStatus newStatus,
                                                final String notes,
                                                final UUID actorUserId,
                                                final String actorName) {

        // Fetch existing appointment
        final Appointment appointment = getAppointment(appointmentId);

        // Capture old status before change
        final AppointmentStatus oldStatus = appointment.getStatus();

        // Validate status transition
        if (oldStatus == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot change status of cancelled appointment");
        }

        // Prevent direct cancel via this endpoint (use dedicated cancel endpoint)
        if (newStatus == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Use the cancel endpoint to cancel appointments");
        }

        // Update status
        appointment.setStatus(newStatus);
        
        // Update notes if provided
        if (notes != null && !notes.isBlank()) {
            final String existingNotes = appointment.getNotes();
            final String updatedNotes = existingNotes != null && !existingNotes.isBlank()
                    ? existingNotes + "\n\n[Status update] " + notes
                    : notes;
            appointment.setNotes(updatedNotes);
        }

        final Appointment saved = appointmentRepository.save(appointment);

        // Async audit trail
        final String metadata = String.format(
                "{\"oldStatus\": \"%s\", \"newStatus\": \"%s\", \"notes\": \"%s\"}",
                oldStatus,
                newStatus,
                notes != null ? notes.replace("\"", "\\\"") : ""
        );

        auditService.recordAuditEntryWithMetadata(
                saved.getId(),
                AuditActionType.STATUS_CHANGED,
                actorUserId,
                actorName,
                metadata
        );

        // Publish event for session record updates
        eventPublisher.publishEvent(AppointmentStatusChangedEvent.of(
                saved.getId(),
                oldStatus,
                newStatus,
                actorUserId,
                actorName
        ));

        LOG.info("Appointment status updated: id={}, oldStatus={}, newStatus={}, actor={}",
                saved.getId(), oldStatus, newStatus, actorName);

        return saved;
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
