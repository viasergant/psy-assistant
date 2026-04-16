package com.psyassistant.sessions.service;

import com.psyassistant.sessions.config.AttendanceProperties;
import com.psyassistant.sessions.domain.AttendanceOutcome;
import com.psyassistant.sessions.domain.FollowUpTask;
import com.psyassistant.sessions.domain.FollowUpTaskType;
import com.psyassistant.sessions.event.AttendanceOutcomeRecordedEvent;
import com.psyassistant.sessions.repository.FollowUpTaskRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Service that creates follow-up tasks when a no-show attendance outcome is recorded.
 *
 * <p>Listens to {@link AttendanceOutcomeRecordedEvent} after the recording transaction commits
 * and creates a {@link FollowUpTask} assigned per configuration.
 *
 * <p>The UNIQUE constraint on {@code session_record_id} prevents duplicate tasks if the
 * event fires more than once (idempotency guard).
 */
@Service
public class NoShowFollowUpService {

    private static final Logger LOG = LoggerFactory.getLogger(NoShowFollowUpService.class);

    private final FollowUpTaskRepository followUpTaskRepository;
    private final AttendanceProperties attendanceProperties;

    /**
     * Constructs the service.
     *
     * @param followUpTaskRepository  follow-up task JPA repository
     * @param attendanceProperties    attendance configuration properties
     */
    public NoShowFollowUpService(
            final FollowUpTaskRepository followUpTaskRepository,
            final AttendanceProperties attendanceProperties) {
        this.followUpTaskRepository = followUpTaskRepository;
        this.attendanceProperties = attendanceProperties;
    }

    /**
     * Handles no-show attendance outcome by creating a follow-up task.
     *
     * <p>Fires after the attendance outcome recording transaction commits.
     * Silently ignores duplicate task creation (idempotency via unique DB constraint).
     *
     * @param event the attendance outcome recorded event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNoShowRecorded(final AttendanceOutcomeRecordedEvent event) {
        if (event.newOutcome() != AttendanceOutcome.NO_SHOW) {
            return;
        }

        if (followUpTaskRepository.existsBySessionRecordId(event.sessionId())) {
            LOG.debug("Follow-up task already exists for session {}, skipping", event.sessionId());
            return;
        }

        final UUID assignedToUserId = resolveAssignedUser(event);

        try {
            final FollowUpTask task = new FollowUpTask(
                    event.sessionId(),
                    assignedToUserId,
                    FollowUpTaskType.NO_SHOW_FOLLOW_UP);

            followUpTaskRepository.save(task);

            LOG.info("No-show follow-up task created: sessionId={}, assignedTo={}",
                    event.sessionId(), assignedToUserId);
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request already created the task — safe to ignore
            LOG.debug("Concurrent follow-up task creation for session {}, ignoring duplicate",
                    event.sessionId());
        }
    }

    private UUID resolveAssignedUser(final AttendanceOutcomeRecordedEvent event) {
        final String assignTo = attendanceProperties.followup().assignTo();
        if ("ADMIN".equalsIgnoreCase(assignTo)) {
            // No specific admin user configured — leave unassigned (admin pool)
            return null;
        }
        // Default: assign to the session therapist
        return event.therapistId();
    }
}
