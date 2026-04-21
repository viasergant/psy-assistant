package com.psyassistant.sessions.service;

import com.psyassistant.sessions.config.AttendanceProperties;
import com.psyassistant.sessions.domain.AttendanceAuditLog;
import com.psyassistant.sessions.domain.AttendanceOutcome;
import com.psyassistant.sessions.domain.GroupSessionAttendance;
import com.psyassistant.sessions.domain.RecordKind;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.dto.RecordGroupAttendanceRequest;
import com.psyassistant.sessions.event.AttendanceOutcomeRecordedEvent;
import com.psyassistant.sessions.repository.AttendanceAuditLogRepository;
import com.psyassistant.sessions.repository.GroupSessionAttendanceRepository;
import com.psyassistant.sessions.repository.SessionParticipantRepository;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording per-client attendance outcomes in GROUP session records.
 *
 * <p>Key design decisions:
 * <ul>
 *     <li>Each client's outcome is saved in {@link GroupSessionAttendance} (not on
 *         {@link SessionRecord} directly, which remains for INDIVIDUAL sessions).</li>
 *     <li>Each client's outcome is processed in a separate {@link Propagation#REQUIRES_NEW}
 *         transaction so that a failure on one client does not roll back others.</li>
 *     <li>Publishes the same {@link AttendanceOutcomeRecordedEvent} contract as
 *         {@link AttendanceOutcomeService} so that {@code NoShowFollowUpService} and
 *         {@code AtRiskEvaluationService} require no changes.</li>
 *     <li>Applies the same late-cancellation window evaluation as the individual flow.</li>
 * </ul>
 */
@Service
public class GroupAttendanceService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupAttendanceService.class);

    private final SessionRecordRepository sessionRecordRepository;
    private final GroupSessionAttendanceRepository attendanceRepository;
    private final AttendanceAuditLogRepository auditLogRepository;
    private final SessionParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttendanceProperties attendanceProperties;

    /**
     * Constructs the service.
     *
     * @param sessionRecordRepository  session record JPA repository
     * @param attendanceRepository     group session attendance JPA repository
     * @param auditLogRepository       attendance audit log JPA repository
     * @param participantRepository    session participant JPA repository
     * @param eventPublisher           Spring application event publisher
     * @param attendanceProperties     attendance configuration properties
     */
    public GroupAttendanceService(
            final SessionRecordRepository sessionRecordRepository,
            final GroupSessionAttendanceRepository attendanceRepository,
            final AttendanceAuditLogRepository auditLogRepository,
            final SessionParticipantRepository participantRepository,
            final ApplicationEventPublisher eventPublisher,
            final AttendanceProperties attendanceProperties) {
        this.sessionRecordRepository = sessionRecordRepository;
        this.attendanceRepository = attendanceRepository;
        this.auditLogRepository = auditLogRepository;
        this.participantRepository = participantRepository;
        this.eventPublisher = eventPublisher;
        this.attendanceProperties = attendanceProperties;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Record outcome for one client
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Records the attendance outcome for a single client within a GROUP session.
     *
     * <p>Executed in its own {@link Propagation#REQUIRES_NEW} transaction so that
     * a failure for this client does not roll back outcomes already saved for other clients.
     *
     * @param sessionId   UUID of the GROUP session record
     * @param clientId    UUID of the client whose outcome is being recorded
     * @param request     attendance outcome request
     * @param actorUserId UUID of the user recording the outcome
     * @throws EntityNotFoundException  if the session or participant not found
     * @throws IllegalArgumentException if the session is not a GROUP record
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public GroupSessionAttendance recordOutcomeForClient(
            final UUID sessionId,
            final UUID clientId,
            final RecordGroupAttendanceRequest request,
            final UUID actorUserId) {

        final SessionRecord session = sessionRecordRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Session record not found: " + sessionId));

        if (session.getRecordKind() != RecordKind.GROUP) {
            throw new IllegalArgumentException(
                    "recordOutcomeForClient is only valid for GROUP session records: " + sessionId);
        }

        // Verify the client is an active participant
        participantRepository.findBySessionRecordIdAndClientId(sessionId, clientId)
                .filter(p -> p.isActive())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client " + clientId + " is not an active participant of session " + sessionId));

        final AttendanceOutcome requestedOutcome = request.outcome();
        final AttendanceOutcome effectiveOutcome = resolveEffectiveOutcome(
                requestedOutcome, request.cancelledAt(), session);

        if (effectiveOutcome != requestedOutcome) {
            LOG.info("Late cancellation downgraded to CANCELLED: sessionId={}, clientId={}, cancelledAt={}",
                    sessionId, clientId, request.cancelledAt());
        }

        // Upsert the attendance record
        final GroupSessionAttendance attendance = attendanceRepository
                .findBySessionRecordIdAndClientId(sessionId, clientId)
                .orElse(new GroupSessionAttendance(sessionId, clientId, effectiveOutcome, actorUserId));

        final AttendanceOutcome previousOutcome = attendance.getAttendanceOutcome();
        attendance.setAttendanceOutcome(effectiveOutcome);
        if (request.cancelledAt() != null) {
            attendance.setCancelledAt(request.cancelledAt());
        }
        if (actorUserId != null) {
            attendance.setCancellationInitiatorId(actorUserId);
        }

        final GroupSessionAttendance saved = attendanceRepository.save(attendance);

        // Write to attendance_audit_log (reuses existing infrastructure)
        final AttendanceAuditLog auditEntry = new AttendanceAuditLog(
                sessionId,
                actorUserId,
                Instant.now(),
                previousOutcome,
                effectiveOutcome);
        if (request.note() != null) {
            auditEntry.setNote(request.note());
        }
        auditLogRepository.save(auditEntry);

        LOG.info("Group attendance recorded: sessionId={}, clientId={}, outcome={}, actor={}",
                sessionId, clientId, effectiveOutcome, actorUserId);

        // Publish event independently per client (PA-41 NoShowFollowUpService requires no changes)
        eventPublisher.publishEvent(new AttendanceOutcomeRecordedEvent(
                sessionId,
                clientId,
                session.getTherapistId(),
                effectiveOutcome,
                previousOutcome,
                request.cancelledAt(),
                actorUserId));

        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bulk — record outcomes for all clients (controller convenience)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Records attendance outcomes for all clients in the given map.
     *
     * <p>Each client is processed independently via {@link #recordOutcomeForClient},
     * which runs in its own {@link Propagation#REQUIRES_NEW} transaction. A failure
     * on one client is logged and skipped; other clients are unaffected.
     *
     * @param sessionId  UUID of the GROUP session record
     * @param outcomes   map of clientId → request for each participant
     * @param actorUserId UUID of the actor
     * @return list of successfully saved attendance records
     */
    @Transactional(readOnly = true)
    public List<GroupSessionAttendance> recordOutcomesForAll(
            final UUID sessionId,
            final java.util.Map<UUID, RecordGroupAttendanceRequest> outcomes,
            final UUID actorUserId) {

        return outcomes.entrySet().stream()
                .map(entry -> {
                    try {
                        return recordOutcomeForClient(sessionId, entry.getKey(),
                                entry.getValue(), actorUserId);
                    } catch (Exception ex) {
                        LOG.error("Failed to record attendance for client {} in session {}: {}",
                                entry.getKey(), sessionId, ex.getMessage(), ex);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers — same logic as AttendanceOutcomeService
    // ─────────────────────────────────────────────────────────────────────

    private AttendanceOutcome resolveEffectiveOutcome(
            final AttendanceOutcome requested,
            final Instant cancelledAt,
            final SessionRecord session) {

        if (requested != AttendanceOutcome.LATE_CANCELLATION || cancelledAt == null) {
            return requested;
        }

        final int windowHours = attendanceProperties.lateCancellation().windowHours();
        if (windowHours <= 0) {
            return requested;
        }

        final LocalDate sessionDate = session.getSessionDate();
        final LocalTime sessionTime = session.getScheduledStartTime();
        final Instant sessionStart = LocalDateTime.of(sessionDate, sessionTime)
                .toInstant(ZoneOffset.UTC);

        final Duration gap = Duration.between(cancelledAt, sessionStart);
        final boolean isWithinWindow = !gap.isNegative() && gap.toHours() < windowHours;

        return isWithinWindow ? AttendanceOutcome.LATE_CANCELLATION : AttendanceOutcome.CANCELLED;
    }
}
