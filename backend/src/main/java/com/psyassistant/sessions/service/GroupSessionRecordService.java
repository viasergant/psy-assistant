package com.psyassistant.sessions.service;

import com.psyassistant.scheduling.domain.Appointment;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing GROUP session records.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Create a single GROUP {@link SessionRecord} from a group appointment with
 *         bulk-inserted participants</li>
 *     <li>Enforce the ≤ 20 participant cap at application layer (DB trigger provides
 *         a second line of defence)</li>
 *     <li>Write audit entries to {@code session_participant_audit} for all adds</li>
 *     <li>Retrieve participants for a group session</li>
 * </ul>
 *
 * <p>Per the constraints, participants are inherited from the appointment at creation time;
 * post-creation additions are not supported. Participant removal is a separate operation
 * that writes an audit entry and hard-deletes the participant timeline entry.
 */
@Service
public class GroupSessionRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupSessionRecordService.class);
    private static final int MAX_PARTICIPANTS = 20;

    private final SessionRecordRepository sessionRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final SessionParticipantRepository participantRepository;
    private final SessionParticipantAuditRepository auditRepository;

    /**
     * Constructs a new GroupSessionRecordService.
     *
     * @param sessionRecordRepository  session record JPA repository
     * @param appointmentRepository    appointment JPA repository
     * @param participantRepository    session participant JPA repository
     * @param auditRepository          session participant audit JPA repository
     */
    public GroupSessionRecordService(
            final SessionRecordRepository sessionRecordRepository,
            final AppointmentRepository appointmentRepository,
            final SessionParticipantRepository participantRepository,
            final SessionParticipantAuditRepository auditRepository) {
        this.sessionRecordRepository = sessionRecordRepository;
        this.appointmentRepository = appointmentRepository;
        this.participantRepository = participantRepository;
        this.auditRepository = auditRepository;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a GROUP session record for the given appointment with the supplied participants.
     *
     * <p>Steps:
     * <ol>
     *     <li>Validate: ≤ 20 participants, ≥ 2 participants for a group session</li>
     *     <li>Validate: no session record already exists for this appointment</li>
     *     <li>Persist the {@link SessionRecord} with {@link RecordKind#GROUP}</li>
     *     <li>Bulk-insert {@link SessionParticipant} rows</li>
     *     <li>Write {@link SessionParticipantAudit} entries (ADDED) for each participant</li>
     * </ol>
     *
     * @param appointmentId appointment UUID
     * @param clientIds     ordered list of client UUIDs (must be between 2 and 20)
     * @param status        initial session status
     * @param actorUserId   user UUID who triggered the creation (for audit)
     * @param actorName     display name of the actor (for audit)
     * @return the persisted GROUP session record
     * @throws EntityNotFoundException if appointment not found
     * @throws IllegalArgumentException if participant count is outside [2, 20]
     * @throws IllegalStateException if a session already exists for this appointment
     */
    @Transactional(rollbackFor = Exception.class)
    public SessionRecord createGroupSession(
            final UUID appointmentId,
            final List<UUID> clientIds,
            final SessionStatus status,
            final UUID actorUserId,
            final String actorName) {

        // Validate participant count
        if (clientIds == null || clientIds.size() < 2) {
            throw new IllegalArgumentException(
                    "A group session requires at least 2 participants");
        }
        if (clientIds.size() > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException(
                    "A group session cannot exceed " + MAX_PARTICIPANTS + " participants; "
                    + "received " + clientIds.size());
        }

        // Check for duplicate session
        if (sessionRecordRepository.existsByAppointmentId(appointmentId)) {
            throw new IllegalStateException(
                    "Session already exists for appointment: " + appointmentId);
        }

        // Load appointment for immutable context
        final Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Appointment not found: " + appointmentId));

        // Create the GROUP session record
        final SessionRecord session = SessionRecord.forGroup(
                appointmentId,
                appointment.getTherapistProfileId(),
                LocalDate.from(appointment.getStartTime()),
                LocalTime.from(appointment.getStartTime()),
                appointment.getSessionType(),
                Duration.ofMinutes(appointment.getDurationMinutes()),
                status
        );

        final SessionRecord savedSession;
        try {
            savedSession = sessionRecordRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            LOG.warn("Concurrent group session creation for appointment {}", appointmentId);
            throw new IllegalStateException(
                    "Concurrent session creation for appointment: " + appointmentId, e);
        }

        LOG.info("GROUP session record created: id={}, appointmentId={}, participantCount={}",
                savedSession.getId(), appointmentId, clientIds.size());

        // Bulk-insert participants + write audit entries
        bulkInsertParticipants(savedSession.getId(), clientIds, actorUserId, actorName);

        return savedSession;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the active participant list for a group session.
     *
     * @param sessionId session record UUID
     * @return list of active participants, may be empty
     */
    @Transactional(readOnly = true)
    public List<SessionParticipant> getActiveParticipants(final UUID sessionId) {
        return participantRepository.findActiveBySessionRecordId(sessionId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private void bulkInsertParticipants(
            final UUID sessionId,
            final List<UUID> clientIds,
            final UUID actorUserId,
            final String actorName) {

        final Instant now = Instant.now();

        // Bulk-save all participants in a single batch (avoids N+1 round-trips)
        final List<SessionParticipant> participants = clientIds.stream()
                .map(clientId -> new SessionParticipant(sessionId, clientId))
                .toList();

        participantRepository.saveAll(participants);

        // Write audit entries for all additions
        final List<SessionParticipantAudit> auditEntries = clientIds.stream()
                .map(clientId -> new SessionParticipantAudit(
                        sessionId,
                        clientId,
                        SessionParticipantAudit.Action.ADDED,
                        actorUserId,
                        actorName))
                .toList();

        auditRepository.saveAll(auditEntries);

        LOG.debug("Bulk-inserted {} participants for session {}", clientIds.size(), sessionId);
    }
}
