package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.AppointmentAudit;
import com.psyassistant.scheduling.domain.AuditActionType;
import com.psyassistant.scheduling.repository.AppointmentAuditRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording immutable audit log entries for appointment changes.
 *
 * <p><strong>Asynchronous Execution</strong>: All audit writes are {@code @Async} to:
 * <ul>
 *     <li>Prevent audit failures from blocking business operations</li>
 *     <li>Avoid performance impact on appointment booking/modification</li>
 *     <li>Ensure audit logging happens in a separate transaction</li>
 * </ul>
 *
 * <p><strong>Transaction Isolation</strong>: Uses {@code REQUIRES_NEW} propagation to ensure
 * audit entries are committed even if the main transaction rolls back (for forensics).
 */
@Service
public class AppointmentAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentAuditService.class);

    private final AppointmentAuditRepository auditRepository;

    public AppointmentAuditService(final AppointmentAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Records an audit entry asynchronously.
     *
     * <p>Fire-and-forget operation. Failures are logged but do not affect the calling transaction.
     *
     * @param appointmentId appointment UUID
     * @param actionType type of action performed
     * @param actorUserId user who performed the action
     * @param actorName display name of the actor
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuditEntry(final UUID appointmentId,
                                   final AuditActionType actionType,
                                   final UUID actorUserId,
                                   final String actorName) {
        try {
            final AppointmentAudit audit = new AppointmentAudit(
                    appointmentId,
                    actionType,
                    actorUserId,
                    actorName
            );
            auditRepository.save(audit);
            LOG.debug("Audit entry created: appointmentId={}, actionType={}, actor={}",
                    appointmentId, actionType, actorName);
        } catch (final Exception e) {
            // Log failure but don't propagate - audit failures should not block business operations
            LOG.error("Failed to create audit entry: appointmentId={}, actionType={}, actor={}",
                    appointmentId, actionType, actorName, e);
        }
    }

    /**
     * Records an audit entry with additional metadata (for conflict overrides, detailed changes).
     *
     * @param appointmentId appointment UUID
     * @param actionType type of action performed
     * @param actorUserId user who performed the action
     * @param actorName display name of the actor
     * @param metadata additional structured context as JSON string
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuditEntryWithMetadata(final UUID appointmentId,
                                               final AuditActionType actionType,
                                               final UUID actorUserId,
                                               final String actorName,
                                               final String metadata) {
        try {
            final AppointmentAudit audit = new AppointmentAudit.Builder(
                    appointmentId,
                    actionType,
                    actorUserId,
                    actorName
            )
                    .metadata(metadata)
                    .build();

            auditRepository.save(audit);
            LOG.debug("Audit entry with metadata created: appointmentId={}, actionType={}, actor={}",
                    appointmentId, actionType, actorName);
        } catch (final Exception e) {
            LOG.error("Failed to create audit entry with metadata: appointmentId={}, actionType={}, actor={}",
                    appointmentId, actionType, actorName, e);
        }
    }

    /**
     * Records an audit entry with field-level change tracking.
     *
     * @param appointmentId appointment UUID
     * @param actionType type of action performed
     * @param actorUserId user who performed the action
     * @param actorName display name of the actor
     * @param fieldName field that changed
     * @param oldValue value before change
     * @param newValue value after change
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFieldChange(final UUID appointmentId,
                                    final AuditActionType actionType,
                                    final UUID actorUserId,
                                    final String actorName,
                                    final String fieldName,
                                    final String oldValue,
                                    final String newValue) {
        try {
            final AppointmentAudit audit = new AppointmentAudit.Builder(
                    appointmentId,
                    actionType,
                    actorUserId,
                    actorName
            )
                    .fieldChange(fieldName, oldValue, newValue)
                    .build();

            auditRepository.save(audit);
            LOG.debug("Audit field change created: appointmentId={}, field={}, actor={}",
                    appointmentId, fieldName, actorName);
        } catch (final Exception e) {
            LOG.error("Failed to create audit field change: appointmentId={}, field={}, actor={}",
                    appointmentId, fieldName, actorName, e);
        }
    }
}
