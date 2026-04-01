package com.psyassistant.sessions.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.event.AppointmentStatusChangedEvent;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing session record lifecycle operations.
 *
 * <p>Provides:
 * <ul>
 *     <li>Automatic session record creation via event listener when appointments
 *         transition to COMPLETED or IN_PROGRESS</li>
 *     <li>Manual session start from appointments</li>
 *     <li>Duplicate prevention via unique constraint on appointment_id</li>
 *     <li>Session cancellation when appointment is cancelled</li>
 * </ul>
 *
 * <p><strong>Event-Driven Architecture</strong>: Listens to {@link AppointmentStatusChangedEvent}
 * to trigger session record creation automatically within the same transaction as the
 * appointment status change.
 */
@Service
public class SessionRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRecordService.class);

    private final SessionRecordRepository sessionRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SessionRecordService(final SessionRecordRepository sessionRecordRepository,
                                 final AppointmentRepository appointmentRepository,
                                 final ApplicationEventPublisher eventPublisher) {
        this.sessionRecordRepository = sessionRecordRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Event listener that automatically creates session records when appointments
     * transition to terminal or active status.
     *
     * <p>Triggers on:
     * <ul>
     *     <li>COMPLETED: Creates session with status PENDING</li>
     *     <li>IN_PROGRESS: Creates session with status IN_PROGRESS</li>
     *     <li>CANCELLED: Cancels existing pending/in-progress sessions</li>
     * </ul>
     *
     * <p>Duplicate prevention: If a session already exists for the appointment,
     * logs a warning and skips creation.
     *
     * @param event appointment status change event
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleAppointmentStatusChanged(final AppointmentStatusChangedEvent event) {
        LOG.debug("Handling appointment status change: {} -> {} for appointment {}",
                event.oldStatus(), event.newStatus(), event.appointmentId());

        final AppointmentStatus newStatus = event.newStatus();

        // Handle session creation for COMPLETED or IN_PROGRESS
        if (newStatus == AppointmentStatus.COMPLETED || newStatus == AppointmentStatus.IN_PROGRESS) {
            createSessionIfNotExists(event);
        }

        // Handle session cancellation for CANCELLED appointments
        if (newStatus == AppointmentStatus.CANCELLED) {
            cancelPendingOrInProgressSession(event.appointmentId(), "Appointment was cancelled");
        }
    }

    /**
     * Creates a session record from an appointment if one doesn't already exist.
     *
     * @param event appointment status change event
     */
    private void createSessionIfNotExists(final AppointmentStatusChangedEvent event) {
        // Check if session already exists
        if (sessionRecordRepository.existsByAppointmentId(event.appointmentId())) {
            LOG.warn("Duplicate session creation attempt for appointment {}, skipping",
                    event.appointmentId());
            return;
        }

        // Fetch appointment to extract session context
        final Appointment appointment = appointmentRepository.findById(event.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Appointment not found: " + event.appointmentId()));

        // Determine session status based on appointment status
        final SessionStatus sessionStatus = event.newStatus() == AppointmentStatus.IN_PROGRESS
                ? SessionStatus.IN_PROGRESS
                : SessionStatus.PENDING;

        // Create session record with immutable fields from appointment
        final SessionRecord sessionRecord = new SessionRecord(
                appointment.getId(),
                appointment.getClientId(),
                appointment.getTherapistProfileId(),
                LocalDate.from(appointment.getStartTime()),
                LocalTime.from(appointment.getStartTime()),
                appointment.getSessionType(),
                Duration.ofMinutes(appointment.getDurationMinutes()),
                sessionStatus
        );

        try {
            final SessionRecord saved = sessionRecordRepository.save(sessionRecord);
            LOG.info("Session record created: id={}, appointmentId={}, status={}",
                    saved.getId(), saved.getAppointmentId(), saved.getStatus());
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created the session first
            LOG.warn("Concurrent session creation detected for appointment {}, likely handled by another transaction",
                    event.appointmentId());
        }
    }

    /**
     * Cancels an existing pending or in-progress session for an appointment.
     *
     * @param appointmentId appointment UUID
     * @param reason cancellation reason
     */
    private void cancelPendingOrInProgressSession(final UUID appointmentId, final String reason) {
        sessionRecordRepository.findByAppointmentId(appointmentId).ifPresent(session -> {
            if (session.getStatus() == SessionStatus.PENDING
                    || session.getStatus() == SessionStatus.IN_PROGRESS) {
                session.cancel(reason);
                sessionRecordRepository.save(session);
                LOG.info("Session cancelled: id={}, appointmentId={}, reason={}",
                        session.getId(), appointmentId, reason);
            }
        });
    }

    /**
     * Manually starts a session from an appointment.
     *
     * <p>Creates a session record with status IN_PROGRESS and updates the appointment
     * status to IN_PROGRESS if it was previously SCHEDULED.
     *
     * <p>Duplicate prevention: Returns HTTP 409 if a session already exists for the appointment.
     *
     * @param appointmentId appointment UUID
     * @param actorUserId user UUID who is starting the session
     * @param actorName display name of the actor
     * @return created session record
     * @throws EntityNotFoundException if appointment not found
     * @throws IllegalStateException if session already exists (HTTP 409)
     */
    @Transactional(rollbackFor = Exception.class)
    public SessionRecord startSession(final UUID appointmentId,
                                       final UUID actorUserId,
                                       final String actorName) {

        // Check for duplicate
        if (sessionRecordRepository.existsByAppointmentId(appointmentId)) {
            throw new IllegalStateException(
                    "Session already exists for appointment: " + appointmentId);
        }

        // Fetch appointment
        final Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Appointment not found: " + appointmentId));

        // Create session record FIRST (before publishing event)
        final SessionRecord sessionRecord = new SessionRecord(
                appointment.getId(),
                appointment.getClientId(),
                appointment.getTherapistProfileId(),
                LocalDate.from(appointment.getStartTime()),
                LocalTime.from(appointment.getStartTime()),
                appointment.getSessionType(),
                Duration.ofMinutes(appointment.getDurationMinutes()),
                SessionStatus.IN_PROGRESS
        );

        final SessionRecord saved = sessionRecordRepository.save(sessionRecord);

        // Update appointment status to IN_PROGRESS if it was SCHEDULED

        // THEN publish event (event listener will see session already exists and skip duplicate)
        if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
            final AppointmentStatus oldStatus = appointment.getStatus();
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
            appointmentRepository.save(appointment);

            eventPublisher.publishEvent(AppointmentStatusChangedEvent.of(
                    appointmentId,
                    oldStatus,
                    AppointmentStatus.IN_PROGRESS,
                    actorUserId,
                    actorName
            ));

            LOG.info("Appointment status updated to IN_PROGRESS: id={}", appointmentId);
        }

        LOG.info("Session started manually: id={}, appointmentId={}, startedBy={}",
                saved.getId(), appointmentId, actorUserId);

        return saved;
    }

    /**
     * Retrieves a session record by its ID.
     *
     * @param sessionId session UUID
     * @return session record
     * @throws EntityNotFoundException if session not found
     */
    @Transactional(readOnly = true)
    public SessionRecord getSessionRecord(final UUID sessionId) {
        return sessionRecordRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
    }

    /**
     * Retrieves a session record by appointment ID.
     *
     * @param appointmentId appointment UUID
     * @return session record
     * @throws EntityNotFoundException if session not found
     */
    @Transactional(readOnly = true)
    public SessionRecord getSessionByAppointmentId(final UUID appointmentId) {
        return sessionRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No session found for appointment: " + appointmentId));
    }
}
