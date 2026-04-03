package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentAudit;
import com.psyassistant.scheduling.domain.AppointmentSeries;
import com.psyassistant.scheduling.domain.AuditActionType;
import com.psyassistant.scheduling.domain.EditScope;
import com.psyassistant.scheduling.repository.AppointmentAuditRepository;
import java.util.List;
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

    // ========== Recurring Series Audit Methods (PA-33) ==========

    /**
     * Records creation of a recurring appointment series.
     *
     * <p>One audit row is written per saved occurrence so that each appointment
     * has its own audit entry with SERIES_CREATED action type.
     *
     * @param series the created series
     * @param appointments saved appointment occurrences
     * @param actorUserId user who created the series
     * @param actorName display name of the actor
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSeriesCreated(final AppointmentSeries series,
                                     final List<Appointment> appointments,
                                     final UUID actorUserId,
                                     final String actorName) {
        try {
            final String metadata = String.format(
                    "{\"seriesId\": %d, \"recurrenceType\": \"%s\","
                    + " \"totalOccurrences\": %d, \"savedOccurrences\": %d}",
                    series.getId(),
                    series.getRecurrenceType(),
                    series.getTotalOccurrences(),
                    appointments.size()
            );

            for (final Appointment appt : appointments) {
                final AppointmentAudit audit = new AppointmentAudit.Builder(
                        appt.getId(),
                        AuditActionType.SERIES_CREATED,
                        actorUserId,
                        actorName
                )
                        .seriesId(series.getId())
                        .editScope(EditScope.ENTIRE_SERIES)
                        .metadata(metadata)
                        .build();
                auditRepository.save(audit);
            }
            LOG.debug("Series created audit: seriesId={}, occurrences={}, actor={}",
                    series.getId(), appointments.size(), actorName);
        } catch (final Exception e) {
            LOG.error("Failed to record series created audit: seriesId={}, actor={}",
                    series.getId(), actorName, e);
        }
    }

    /**
     * Records a single-occurrence edit (isModified flagged).
     *
     * @param appointment the edited appointment
     * @param scope edit scope (SINGLE expected)
     * @param actorUserId user who performed the edit
     * @param actorName display name of the actor
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOccurrenceEdit(final Appointment appointment,
                                      final EditScope scope,
                                      final UUID actorUserId,
                                      final String actorName) {
        try {
            final Long seriesId = appointment.getSeries() != null ? appointment.getSeries().getId() : null;
            final AppointmentAudit audit = new AppointmentAudit.Builder(
                    appointment.getId(),
                    AuditActionType.SERIES_OCCURRENCE_EDITED,
                    actorUserId,
                    actorName
            )
                    .seriesId(seriesId)
                    .editScope(scope)
                    .build();
            auditRepository.save(audit);
            LOG.debug("Occurrence edit audit: appointmentId={}, scope={}, actor={}",
                    appointment.getId(), scope, actorName);
        } catch (final Exception e) {
            LOG.error("Failed to record occurrence edit audit: appointmentId={}, actor={}",
                    appointment.getId(), actorName, e);
        }
    }

    /**
     * Records a future-series edit (from a given occurrence forward).
     *
     * @param series the affected series
     * @param fromAppointmentId the anchor appointment UUID
     * @param updatedCount number of appointments updated
     * @param actorUserId user who performed the edit
     * @param actorName display name of the actor
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFutureSeriesEdit(final AppointmentSeries series,
                                        final UUID fromAppointmentId,
                                        final int updatedCount,
                                        final UUID actorUserId,
                                        final String actorName) {
        try {
            final String metadata = String.format(
                    "{\"seriesId\": %d, \"fromAppointmentId\": \"%s\", \"updatedCount\": %d}",
                    series.getId(),
                    fromAppointmentId,
                    updatedCount
            );
            final AppointmentAudit audit = new AppointmentAudit.Builder(
                    fromAppointmentId,
                    AuditActionType.SERIES_FUTURE_EDITED,
                    actorUserId,
                    actorName
            )
                    .seriesId(series.getId())
                    .editScope(EditScope.FUTURE_SERIES)
                    .metadata(metadata)
                    .build();
            auditRepository.save(audit);
            LOG.debug("Future series edit audit: seriesId={}, from={}, count={}, actor={}",
                    series.getId(), fromAppointmentId, updatedCount, actorName);
        } catch (final Exception e) {
            LOG.error("Failed to record future series edit audit: seriesId={}, actor={}",
                    series.getId(), actorName, e);
        }
    }

    /**
     * Records a series cancellation (FUTURE_SERIES or ENTIRE_SERIES).
     *
     * @param seriesId the series ID that was cancelled
     * @param fromIndex the recurrence index from which cancellation started
     * @param cancelledCount number of appointments cancelled
     * @param scope cancel scope
     * @param actorUserId user who performed the cancellation
     * @param actorName display name of the actor
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSeriesCancellation(final Long seriesId,
                                          final int fromIndex,
                                          final int cancelledCount,
                                          final EditScope scope,
                                          final UUID actorUserId,
                                          final String actorName) {
        try {
            // For series-level cancellation there may be no single appointment to attach to;
            // use a synthetic UUID derived from seriesId as the appointmentId field for correlation.
            final UUID correlationId = UUID.nameUUIDFromBytes(
                    ("series-cancel-" + seriesId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final String metadata = String.format(
                    "{\"seriesId\": %d, \"fromIndex\": %d, \"cancelledCount\": %d, \"scope\": \"%s\"}",
                    seriesId, fromIndex, cancelledCount, scope
            );
            final AppointmentAudit audit = new AppointmentAudit.Builder(
                    correlationId,
                    AuditActionType.SERIES_CANCELLED,
                    actorUserId,
                    actorName
            )
                    .seriesId(seriesId)
                    .editScope(scope)
                    .metadata(metadata)
                    .build();
            auditRepository.save(audit);
            LOG.debug("Series cancellation audit: seriesId={}, scope={}, cancelled={}, actor={}",
                    seriesId, scope, cancelledCount, actorName);
        } catch (final Exception e) {
            LOG.error("Failed to record series cancellation audit: seriesId={}, actor={}",
                    seriesId, actorName, e);
        }
    }
}
