package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentSeries;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.ConflictResolution;
import com.psyassistant.scheduling.domain.EditScope;
import com.psyassistant.scheduling.domain.RecurrenceType;
import com.psyassistant.scheduling.domain.SeriesStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.dto.CancelRecurringOccurrenceRequest;
import com.psyassistant.scheduling.dto.CreateRecurringSeriesRequest;
import com.psyassistant.scheduling.dto.CreateRecurringSeriesResponse;
import com.psyassistant.scheduling.dto.EditRecurringOccurrenceRequest;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse.ConflictDetail;
import com.psyassistant.scheduling.event.RecurringSlotSkippedEvent;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.AppointmentSeriesRepository;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
 * Service for managing recurring appointment series lifecycle.
 *
 * <p>Implements Phase 1 (MVP) of PA-33:
 * <ul>
 *     <li>Conflict pre-flight check across all generated slots</li>
 *     <li>Atomic series creation with SKIP_CONFLICTS or ABORT resolution</li>
 *     <li>Single occurrence edit (reschedule / notes update)</li>
 *     <li>Future-series edit (from selected occurrence forward)</li>
 *     <li>Single occurrence cancellation (delegates to {@link AppointmentService})</li>
 *     <li>Bulk cancellation (FUTURE_SERIES / ENTIRE_SERIES) via atomic bulk UPDATE</li>
 * </ul>
 *
 * <p>All mutating methods are {@code @Retryable} on {@link OptimisticLockException}
 * to match the pattern established in {@link AppointmentService}.
 */
@Service
public class RecurringSeriesService {

    private static final Logger LOG = LoggerFactory.getLogger(RecurringSeriesService.class);

    private final RecurrencePatternGenerator patternGenerator;
    private final BatchConflictDetectionService batchConflictDetection;
    private final AppointmentSeriesRepository seriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final AppointmentAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public RecurringSeriesService(final RecurrencePatternGenerator patternGenerator,
                                   final BatchConflictDetectionService batchConflictDetection,
                                   final AppointmentSeriesRepository seriesRepository,
                                   final AppointmentRepository appointmentRepository,
                                   final SessionTypeRepository sessionTypeRepository,
                                   final AppointmentAuditService auditService,
                                   final ApplicationEventPublisher eventPublisher) {
        this.patternGenerator = patternGenerator;
        this.batchConflictDetection = batchConflictDetection;
        this.seriesRepository = seriesRepository;
        this.appointmentRepository = appointmentRepository;
        this.sessionTypeRepository = sessionTypeRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    // ========== Conflict Pre-Flight ==========

    /**
     * Checks every generated slot for conflicts without creating any records.
     *
     * @param therapistProfileId therapist UUID
     * @param startTime first occurrence start time
     * @param durationMinutes duration of each occurrence
     * @param timezone IANA timezone identifier
     * @param recurrenceType spacing between occurrences
     * @param occurrences total occurrences requested
     * @return per-slot conflict results with aggregate counts
     */
    @Transactional(readOnly = true)
    public RecurringConflictCheckResponse checkConflicts(final UUID therapistProfileId,
                                                          final ZonedDateTime startTime,
                                                          final int durationMinutes,
                                                          final String timezone,
                                                          final RecurrenceType recurrenceType,
                                                          final int occurrences) {

        final List<ZonedDateTime> slots = patternGenerator.generate(startTime, recurrenceType, occurrences);
        final Map<Integer, ConflictDetail> conflicts =
                batchConflictDetection.detectBatch(therapistProfileId, slots, durationMinutes);

        final List<RecurringConflictCheckResponse.RecurringSlotCheckResult> results = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            final ConflictDetail detail = conflicts.get(i);
            results.add(new RecurringConflictCheckResponse.RecurringSlotCheckResult(
                    i, slots.get(i), detail != null, detail));
        }

        return new RecurringConflictCheckResponse(
                List.copyOf(results),
                conflicts.size(),
                slots.size() - conflicts.size()
        );
    }

    // ========== Series Creation ==========

    /**
     * Creates a new recurring appointment series and all its occurrences.
     *
     * <p><strong>Atomicity</strong>: The series record and all appointment rows are saved in
     * a single transaction.  Conflicting slots are either skipped (SKIP_CONFLICTS) or cause
     * an abort (ABORT) before any DB writes occur.
     *
     * @param req creation request including conflict resolution choice
     * @param staffUserId user creating the series
     * @param staffName display name for audit
     * @return summary of saved and skipped occurrences
     * @throws SeriesConflictException when resolution is ABORT and at least one conflict exists
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public CreateRecurringSeriesResponse createSeries(final CreateRecurringSeriesRequest req,
                                                       final UUID staffUserId,
                                                       final String staffName) {

        // 1. Validate session type
        final SessionType sessionType = sessionTypeRepository.findById(req.sessionTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Session type not found: " + req.sessionTypeId()));

        // 2. Generate slots
        final List<ZonedDateTime> slots = patternGenerator.generate(
                req.startTime(), req.recurrenceType(), req.occurrences());

        // 3. Batch conflict detection
        final Map<Integer, ConflictDetail> conflictMap =
                batchConflictDetection.detectBatch(
                        req.therapistProfileId(), slots, req.durationMinutes());

        // 4. Handle ABORT resolution
        if (!conflictMap.isEmpty() && req.conflictResolution() == ConflictResolution.ABORT) {
            LOG.warn("Series creation aborted due to conflicts: therapist={}, conflictCount={}",
                    req.therapistProfileId(), conflictMap.size());
            throw new SeriesConflictException(
                    "Series has " + conflictMap.size() + " conflicting slot(s). Aborting.",
                    conflictMap);
        }

        // 5. Create series parent record
        final AppointmentSeries series = new AppointmentSeries(
                req.recurrenceType(),
                req.startTime().toLocalDate(),
                req.occurrences(),
                req.therapistProfileId(),
                req.clientId(),
                sessionType,
                req.durationMinutes(),
                req.timezone(),
                staffUserId
        );
        final AppointmentSeries savedSeries = seriesRepository.save(series);

        // 6. Create appointment occurrences (skip conflicting ones)
        final List<Appointment> saved = new ArrayList<>();
        final List<Integer> skippedIndices = new ArrayList<>();

        for (int i = 0; i < slots.size(); i++) {
            if (conflictMap.containsKey(i)) {
                skippedIndices.add(i);
                continue;
            }

            final Appointment appt = new Appointment(
                    req.therapistProfileId(),
                    req.clientId(),
                    sessionType,
                    slots.get(i),
                    req.durationMinutes(),
                    req.timezone()
            );
            appt.setSeries(savedSeries);
            appt.setRecurrenceIndex(i);
            if (req.notes() != null && !req.notes().isBlank()) {
                appt.setNotes(req.notes());
            }
            saved.add(appointmentRepository.save(appt));
        }

        // 7. Update generated count on the series
        savedSeries.setGeneratedOccurrences(saved.size());
        seriesRepository.save(savedSeries);

        // 8. Publish events for skipped slots
        for (final int idx : skippedIndices) {
            eventPublisher.publishEvent(new RecurringSlotSkippedEvent(
                    savedSeries.getId(),
                    req.therapistProfileId(),
                    req.clientId(),
                    slots.get(idx),
                    req.durationMinutes(),
                    idx
            ));
        }

        // 9. Async audit
        auditService.recordSeriesCreated(savedSeries, saved, staffUserId, staffName);

        LOG.info("Recurring series created: id={}, saved={}, skipped={}",
                savedSeries.getId(), saved.size(), skippedIndices.size());

        return new CreateRecurringSeriesResponse(
                savedSeries.getId(),
                req.occurrences(),
                saved.size(),
                skippedIndices.size(),
                Collections.emptyList(), // populated by controller via mapper
                Collections.emptyList()  // waitlist IDs — PA-34 stub
        );
    }

    // ========== Get Series ==========

    /**
     * Retrieves a series record and all its appointments.
     *
     * @param seriesId series ID
     * @return series entity (caller maps to DTO)
     * @throws EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public AppointmentSeries getSeries(final Long seriesId) {
        return seriesRepository.findById(seriesId)
                .orElseThrow(() -> new EntityNotFoundException("Series not found: " + seriesId));
    }

    /**
     * Retrieves all appointments belonging to a series.
     *
     * @param seriesId series ID
     * @return appointments ordered by recurrence index
     */
    @Transactional(readOnly = true)
    public List<Appointment> getSeriesAppointments(final Long seriesId) {
        return appointmentRepository.findAllBySeriesId(seriesId);
    }

    // ========== Edit Occurrence ==========

    /**
     * Edits a single occurrence or all future occurrences of a series.
     *
     * @param seriesId parent series ID
     * @param appointmentId the appointment being edited (anchor for FUTURE_SERIES)
     * @param req edit request
     * @param staffUserId actor user ID
     * @param staffName actor display name
     * @return the edited appointment (SINGLE scope) or first edited appointment (FUTURE_SERIES)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public Appointment editOccurrence(final Long seriesId,
                                       final UUID appointmentId,
                                       final EditRecurringOccurrenceRequest req,
                                       final UUID staffUserId,
                                       final String staffName) {

        final Appointment anchor = getAppointmentInSeries(appointmentId, seriesId);

        if (req.editScope() == EditScope.SINGLE) {
            return editSingleOccurrence(anchor, req, staffUserId, staffName);
        }

        // FUTURE_SERIES
        return editFutureSeries(seriesId, anchor, req, staffUserId, staffName);
    }

    private Appointment editSingleOccurrence(final Appointment appt,
                                              final EditRecurringOccurrenceRequest req,
                                              final UUID staffUserId,
                                              final String staffName) {
        if (req.startTime() != null) {
            appt.setStartTime(req.startTime());
        }
        if (req.durationMinutes() != null) {
            appt.setDurationMinutes(req.durationMinutes());
        }
        if (req.notes() != null) {
            appt.setNotes(req.notes());
        }
        appt.setModified(true);

        final Appointment saved = appointmentRepository.save(appt);
        auditService.recordOccurrenceEdit(saved, EditScope.SINGLE, staffUserId, staffName);

        LOG.info("Single occurrence edited: appointmentId={}, seriesId={}",
                saved.getId(), saved.getSeries() != null ? saved.getSeries().getId() : null);
        return saved;
    }

    private Appointment editFutureSeries(final Long seriesId,
                                          final Appointment anchor,
                                          final EditRecurringOccurrenceRequest req,
                                          final UUID staffUserId,
                                          final String staffName) {
        final int fromIndex = anchor.getRecurrenceIndex() != null ? anchor.getRecurrenceIndex() : 0;
        final List<Appointment> futureAppts = appointmentRepository.findBySeriesIdFromIndex(
                seriesId, fromIndex, AppointmentStatus.CANCELLED);

        for (final Appointment appt : futureAppts) {
            if (req.startTime() != null) {
                // For FUTURE_SERIES we shift all slots by the same delta relative to anchor
                final long deltaMinutes = java.time.Duration.between(
                        anchor.getStartTime(), req.startTime()).toMinutes();
                appt.setStartTime(appt.getStartTime().plusMinutes(deltaMinutes));
            }
            if (req.durationMinutes() != null) {
                appt.setDurationMinutes(req.durationMinutes());
            }
            if (req.notes() != null) {
                appt.setNotes(req.notes());
            }
        }
        appointmentRepository.saveAll(futureAppts);

        final AppointmentSeries series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new EntityNotFoundException("Series not found: " + seriesId));
        auditService.recordFutureSeriesEdit(series, anchor.getId(), futureAppts.size(), staffUserId, staffName);

        LOG.info("Future series edited: seriesId={}, fromIndex={}, count={}",
                seriesId, fromIndex, futureAppts.size());
        return futureAppts.isEmpty() ? anchor : futureAppts.get(0);
    }

    // ========== Cancel Occurrences ==========

    /**
     * Cancels one or more occurrences in a series.
     *
     * <p>SINGLE scope delegates to {@link AppointmentService#cancelAppointment}.
     * FUTURE_SERIES and ENTIRE_SERIES use atomic bulk UPDATE via repository.
     *
     * @param seriesId parent series ID
     * @param appointmentId the appointment being cancelled (anchor for FUTURE_SERIES / ENTIRE_SERIES)
     * @param req cancellation request
     * @param staffUserId actor user ID
     * @param staffName actor display name
     * @param appointmentService delegate for single-occurrence cancellation
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void cancelOccurrences(final Long seriesId,
                                   final UUID appointmentId,
                                   final CancelRecurringOccurrenceRequest req,
                                   final UUID staffUserId,
                                   final String staffName,
                                   final AppointmentService appointmentService) {

        if (req.cancelScope() == EditScope.SINGLE) {
            appointmentService.cancelAppointment(
                    appointmentId,
                    req.cancellationType(),
                    req.cancellationReason(),
                    staffUserId,
                    staffName
            );
            return;
        }

        final Appointment anchor = getAppointmentInSeries(appointmentId, seriesId);
        final int fromIndex = req.cancelScope() == EditScope.ENTIRE_SERIES
                ? 0
                : (anchor.getRecurrenceIndex() != null ? anchor.getRecurrenceIndex() : 0);

        final int cancelledCount = appointmentRepository.bulkCancelFromIndex(
                seriesId,
                fromIndex,
                req.cancellationReason(),
                req.cancellationType().name()
        );

        // Update series status
        final AppointmentSeries series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new EntityNotFoundException("Series not found: " + seriesId));

        final long remaining = appointmentRepository.findBySeriesIdFromIndex(seriesId, 0, AppointmentStatus.CANCELLED)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .count();
        series.updateStatus(remaining == 0 ? SeriesStatus.CANCELLED : SeriesStatus.PARTIALLY_CANCELLED);
        seriesRepository.save(series);

        auditService.recordSeriesCancellation(
                seriesId, fromIndex, cancelledCount, req.cancelScope(), staffUserId, staffName);

        LOG.info("Bulk cancel: seriesId={}, fromIndex={}, cancelledCount={}, scope={}",
                seriesId, fromIndex, cancelledCount, req.cancelScope());
    }

    // ========== Helpers ==========

    private Appointment getAppointmentInSeries(final UUID appointmentId, final Long seriesId) {
        final Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found: " + appointmentId));
        if (appt.getSeries() == null || !seriesId.equals(appt.getSeries().getId())) {
            throw new IllegalArgumentException(
                    "Appointment " + appointmentId + " does not belong to series " + seriesId);
        }
        return appt;
    }

    // ========== Exception Types ==========

    /**
     * Thrown when series creation is aborted because conflicts exist and ABORT resolution was chosen.
     */
    public static class SeriesConflictException extends RuntimeException {
        private final Map<Integer, ConflictDetail> conflictMap;

        public SeriesConflictException(final String message, final Map<Integer, ConflictDetail> conflictMap) {
            super(message);
            this.conflictMap = conflictMap;
        }

        public Map<Integer, ConflictDetail> getConflictMap() {
            return conflictMap;
        }
    }
}
