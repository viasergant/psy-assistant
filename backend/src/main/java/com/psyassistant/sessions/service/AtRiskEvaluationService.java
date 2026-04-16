package com.psyassistant.sessions.service;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.sessions.config.AttendanceProperties;
import com.psyassistant.sessions.event.AttendanceOutcomeRecordedEvent;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Service that evaluates the at-risk flag on a client whenever an attendance outcome is recorded.
 *
 * <p>Listens to {@link AttendanceOutcomeRecordedEvent} after the recording transaction commits
 * and re-evaluates whether the client's no-show count meets or exceeds the configured threshold
 * within the configured lookback window.
 *
 * <p>Uses {@code @Retryable} to handle {@link ObjectOptimisticLockingFailureException} caused by
 * concurrent updates to the {@link Client} entity (which has a JPA {@code @Version} field).
 */
@Service
public class AtRiskEvaluationService {

    private static final Logger LOG = LoggerFactory.getLogger(AtRiskEvaluationService.class);

    private final SessionRecordRepository sessionRecordRepository;
    private final ClientRepository clientRepository;
    private final AttendanceProperties attendanceProperties;

    /**
     * Constructs the service.
     *
     * @param sessionRecordRepository session record JPA repository
     * @param clientRepository        client JPA repository
     * @param attendanceProperties    attendance configuration properties
     */
    public AtRiskEvaluationService(
            final SessionRecordRepository sessionRecordRepository,
            final ClientRepository clientRepository,
            final AttendanceProperties attendanceProperties) {
        this.sessionRecordRepository = sessionRecordRepository;
        this.clientRepository = clientRepository;
        this.attendanceProperties = attendanceProperties;
    }

    /**
     * Evaluates and updates the at-risk flag for the client associated with the event.
     *
     * <p>Fires after the attendance outcome recording transaction commits.
     * Re-evaluates no-show count and updates {@code isAtRisk} accordingly.
     * The flag is cleared automatically when the count drops below threshold.
     *
     * @param event the attendance outcome recorded event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public void onAttendanceOutcomeRecorded(final AttendanceOutcomeRecordedEvent event) {
        evaluateAtRiskFlag(event.clientId());
    }

    /**
     * Evaluates and updates the at-risk flag for a specific client.
     *
     * @param clientId the client UUID to evaluate
     */
    private void evaluateAtRiskFlag(final java.util.UUID clientId) {
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientId));

        final int threshold = attendanceProperties.noShow().threshold();
        final int lookbackDays = attendanceProperties.noShow().lookbackDays();

        final LocalDate lookbackFrom = lookbackDays > 0
                ? LocalDate.now().minusDays(lookbackDays)
                : null;

        final long noShowCount = sessionRecordRepository.countNoShowsForClient(clientId, lookbackFrom);
        final boolean shouldBeAtRisk = noShowCount >= threshold;

        if (client.isAtRisk() != shouldBeAtRisk) {
            client.setAtRisk(shouldBeAtRisk);
            clientRepository.save(client);

            LOG.info("Client at-risk flag updated: clientId={}, isAtRisk={}, noShowCount={}, threshold={}",
                    clientId, shouldBeAtRisk, noShowCount, threshold);
        }
    }
}
