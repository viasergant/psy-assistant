package com.psyassistant.sessions.service;

import com.psyassistant.sessions.config.AttendanceProperties;
import com.psyassistant.sessions.domain.AttendanceAuditLog;
import com.psyassistant.sessions.domain.AttendanceOutcome;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.dto.AttendanceOutcomeResponse;
import com.psyassistant.sessions.dto.RecordAttendanceRequest;
import com.psyassistant.sessions.event.AttendanceOutcomeRecordedEvent;
import com.psyassistant.sessions.repository.AttendanceAuditLogRepository;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording and managing session attendance outcomes.
 *
 * <p>Handles:
 * <ul>
 *     <li>Persistence of the attendance outcome on the session record</li>
 *     <li>Late cancellation policy evaluation (downgrade to CANCELLED when outside window)</li>
 *     <li>Attendance audit log entry creation</li>
 *     <li>Publishing {@link AttendanceOutcomeRecordedEvent} for downstream listeners</li>
 * </ul>
 */
@Service
public class AttendanceOutcomeService {

    private static final Logger LOG = LoggerFactory.getLogger(AttendanceOutcomeService.class);

    private final SessionRecordRepository sessionRecordRepository;
    private final AttendanceAuditLogRepository attendanceAuditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttendanceProperties attendanceProperties;

    /**
     * Constructs the service.
     *
     * @param sessionRecordRepository     session record JPA repository
     * @param attendanceAuditLogRepository audit log JPA repository
     * @param eventPublisher              Spring application event publisher
     * @param attendanceProperties        attendance configuration properties
     */
    public AttendanceOutcomeService(
            final SessionRecordRepository sessionRecordRepository,
            final AttendanceAuditLogRepository attendanceAuditLogRepository,
            final ApplicationEventPublisher eventPublisher,
            final AttendanceProperties attendanceProperties) {
        this.sessionRecordRepository = sessionRecordRepository;
        this.attendanceAuditLogRepository = attendanceAuditLogRepository;
        this.eventPublisher = eventPublisher;
        this.attendanceProperties = attendanceProperties;
    }

    /**
     * Records an attendance outcome for a session.
     *
     * <p>If the requested outcome is {@code LATE_CANCELLATION} but the {@code cancelledAt}
     * timestamp falls outside the configured late-cancellation window, the effective outcome
     * is downgraded to {@code CANCELLED} automatically.
     *
     * @param sessionId   UUID of the session record
     * @param request     the attendance outcome request
     * @param actorUserId UUID of the user recording the outcome
     * @return response containing effective outcome and metadata
     * @throws EntityNotFoundException if the session record is not found
     */
    @Transactional
    public AttendanceOutcomeResponse recordOutcome(
            final UUID sessionId,
            final RecordAttendanceRequest request,
            final UUID actorUserId) {

        final SessionRecord session = sessionRecordRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session record not found: " + sessionId));

        final AttendanceOutcome previousOutcome = session.getAttendanceOutcome();
        final AttendanceOutcome requestedOutcome = request.outcome();

        // Evaluate late cancellation policy
        final AttendanceOutcome effectiveOutcome = resolveEffectiveOutcome(
                requestedOutcome, request.cancelledAt(), session);

        if (effectiveOutcome != requestedOutcome) {
            LOG.info(
                    "Late cancellation downgraded to CANCELLED: sessionId={}, cancelledAt={}",
                    sessionId, request.cancelledAt());
        }

        // Update session record
        session.setAttendanceOutcome(effectiveOutcome);
        if (request.cancelledAt() != null) {
            session.setCancelledAt(request.cancelledAt());
        }
        if (actorUserId != null) {
            session.setCancellationInitiatorId(actorUserId);
        }

        sessionRecordRepository.save(session);

        // Append to audit log
        final AttendanceAuditLog auditEntry = new AttendanceAuditLog(
                sessionId,
                actorUserId,
                Instant.now(),
                previousOutcome,
                effectiveOutcome);
        if (request.note() != null) {
            auditEntry.setNote(request.note());
        }
        attendanceAuditLogRepository.save(auditEntry);

        LOG.info("Attendance outcome recorded: sessionId={}, outcome={}, actor={}",
                sessionId, effectiveOutcome, actorUserId);

        // Publish event for downstream listeners (after save, within transaction)
        eventPublisher.publishEvent(new AttendanceOutcomeRecordedEvent(
                sessionId,
                session.getClientId(),
                session.getTherapistId(),
                effectiveOutcome,
                previousOutcome,
                request.cancelledAt(),
                actorUserId));

        return new AttendanceOutcomeResponse(
                sessionId,
                effectiveOutcome,
                requestedOutcome,
                request.cancelledAt(),
                session.getUpdatedAt());
    }

    /**
     * Resolves the effective outcome, applying late-cancellation window enforcement.
     *
     * <p>If outcome is {@code LATE_CANCELLATION}:
     * <ul>
     *     <li>If {@code cancelledAt} is null — treated as LATE_CANCELLATION (no timestamp to evaluate)</li>
     *     <li>If {@code cancelledAt} is within the configured window before session start — LATE_CANCELLATION</li>
     *     <li>If {@code cancelledAt} is outside the window — downgraded to CANCELLED</li>
     * </ul>
     */
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

        // Compute session start as Instant (use UTC for comparison)
        final LocalDate sessionDate = session.getSessionDate();
        final LocalTime sessionTime = session.getScheduledStartTime();
        final Instant sessionStart = LocalDateTime.of(sessionDate, sessionTime)
                .toInstant(ZoneOffset.UTC);

        final Duration gap = Duration.between(cancelledAt, sessionStart);
        final boolean isWithinWindow = !gap.isNegative() && gap.toHours() < windowHours;

        return isWithinWindow ? AttendanceOutcome.LATE_CANCELLATION : AttendanceOutcome.CANCELLED;
    }
}
