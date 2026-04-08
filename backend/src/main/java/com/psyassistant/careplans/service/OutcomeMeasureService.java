package com.psyassistant.careplans.service;

import com.psyassistant.careplans.domain.AlertSeverity;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanStatus;
import com.psyassistant.careplans.domain.OutcomeMeasureDefinition;
import com.psyassistant.careplans.domain.OutcomeMeasureEntry;
import com.psyassistant.careplans.dto.OutcomeMeasureChartDataPoint;
import com.psyassistant.careplans.dto.OutcomeMeasureChartDataResponse;
import com.psyassistant.careplans.dto.OutcomeMeasureDefinitionResponse;
import com.psyassistant.careplans.dto.OutcomeMeasureEntryResponse;
import com.psyassistant.careplans.dto.RecordOutcomeMeasureRequest;
import com.psyassistant.careplans.exception.CarePlanNotActiveException;
import com.psyassistant.careplans.repository.CarePlanRepository;
import com.psyassistant.careplans.repository.OutcomeMeasureDefinitionRepository;
import com.psyassistant.careplans.repository.OutcomeMeasureEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business logic for recording and querying outcome measure assessments.
 *
 * <p>Entries are immutable once created. Score must be within [minScore, maxScore].
 * Threshold evaluation is denormalised at write time so historical entries remain
 * accurate even if the definition changes.
 */
@Service
public class OutcomeMeasureService {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeMeasureService.class);

    private final CarePlanRepository carePlanRepository;
    private final OutcomeMeasureDefinitionRepository definitionRepository;
    private final OutcomeMeasureEntryRepository entryRepository;
    private final CarePlanAuditService auditService;

    public OutcomeMeasureService(
            final CarePlanRepository carePlanRepository,
            final OutcomeMeasureDefinitionRepository definitionRepository,
            final OutcomeMeasureEntryRepository entryRepository,
            final CarePlanAuditService auditService) {
        this.carePlanRepository = carePlanRepository;
        this.definitionRepository = definitionRepository;
        this.entryRepository = entryRepository;
        this.auditService = auditService;
    }

    // -------------------------------------------------------------------------
    // Definitions
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OutcomeMeasureDefinitionResponse> getDefinitions() {
        return definitionRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toDefinitionResponse).toList();
    }

    // -------------------------------------------------------------------------
    // Record a new entry
    // -------------------------------------------------------------------------

    @Transactional
    public OutcomeMeasureEntryResponse recordEntry(
            final UUID planId,
            final RecordOutcomeMeasureRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final OutcomeMeasureDefinition definition = definitionRepository
                .findById(request.measureDefinitionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Outcome measure definition not found: " + request.measureDefinitionId()));

        // Validate score range
        final int score = request.score();
        if (score < definition.getMinScore() || score > definition.getMaxScore()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Score " + score + " is outside valid range ["
                            + definition.getMinScore() + ", " + definition.getMaxScore() + "]"
                            + " for measure " + definition.getCode());
        }

        // Validate date is not in the future
        if (request.assessmentDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assessment date cannot be in the future.");
        }

        // Evaluate threshold
        final boolean thresholdBreached = definition.getAlertThreshold() != null
                && score >= definition.getAlertThreshold();
        final String alertLabel = thresholdBreached ? definition.getAlertLabel() : null;
        final AlertSeverity alertSeverity = thresholdBreached ? definition.getAlertSeverity() : null;

        final OutcomeMeasureEntry entry = new OutcomeMeasureEntry(
                planId,
                definition.getId(),
                score,
                request.assessmentDate(),
                request.notes(),
                actorId,
                actorName,
                thresholdBreached,
                alertLabel,
                alertSeverity);

        entryRepository.save(entry);
        auditService.recordOutcomeMeasureRecorded(planId, entry.getId(), actorId, actorName);

        LOG.info("Outcome measure recorded: plan={}, measure={}, score={}, actor={}",
                planId, definition.getCode(), score, actorId);

        return toEntryResponse(entry, definition);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<OutcomeMeasureEntryResponse> getEntries(
            final UUID planId,
            final String measureCode,
            final LocalDate from,
            final LocalDate to,
            final UUID actorId,
            final boolean hasReadAll,
            final Pageable pageable) {

        final CarePlan plan = loadPlan(planId);
        verifyAccess(plan, actorId, hasReadAll);

        return entryRepository.findFiltered(planId, measureCode, from, to, pageable)
                .map(e -> {
                    final OutcomeMeasureDefinition def = definitionRepository
                            .findById(e.getMeasureDefinitionId())
                            .orElseThrow();
                    return toEntryResponse(e, def);
                });
    }

    @Transactional(readOnly = true)
    public OutcomeMeasureChartDataResponse getChartData(
            final UUID planId,
            final String measureCode,
            final UUID actorId,
            final boolean hasReadAll) {

        final CarePlan plan = loadPlan(planId);
        verifyAccess(plan, actorId, hasReadAll);

        final OutcomeMeasureDefinition definition = definitionRepository
                .findByCode(measureCode)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Outcome measure not found: " + measureCode));

        final List<OutcomeMeasureChartDataPoint> series =
                entryRepository.findByCarePlanIdAndMeasureDefinitionIdOrderByAssessmentDateAsc(
                                planId, definition.getId())
                        .stream()
                        .map(e -> new OutcomeMeasureChartDataPoint(
                                e.getAssessmentDate(), e.getScore(), e.isThresholdBreached()))
                        .toList();

        return new OutcomeMeasureChartDataResponse(
                definition.getCode(),
                definition.getDisplayName(),
                definition.getAlertThreshold(),
                series);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private CarePlan loadPlan(final UUID planId) {
        return carePlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Care plan not found: " + planId));
    }

    private void requireActive(final CarePlan plan) {
        if (plan.getStatus() != CarePlanStatus.ACTIVE) {
            throw new CarePlanNotActiveException(
                    "Care plan is " + plan.getStatus() + " and cannot be modified.");
        }
    }

    private void verifyOwnership(final CarePlan plan, final UUID actorId) {
        if (!plan.getTherapistId().equals(actorId)) {
            throw new AccessDeniedException("You do not own this care plan.");
        }
    }

    private void verifyAccess(final CarePlan plan, final UUID actorId, final boolean hasReadAll) {
        if (!hasReadAll && !plan.getTherapistId().equals(actorId)) {
            throw new AccessDeniedException("You do not have access to this care plan.");
        }
    }

    private OutcomeMeasureDefinitionResponse toDefinitionResponse(
            final OutcomeMeasureDefinition d) {
        return new OutcomeMeasureDefinitionResponse(
                d.getId(), d.getCode(), d.getDisplayName(), d.getDescription(),
                d.getMinScore(), d.getMaxScore(), d.getAlertThreshold(),
                d.getAlertLabel(), d.getAlertSeverity(), d.getSortOrder());
    }

    private OutcomeMeasureEntryResponse toEntryResponse(
            final OutcomeMeasureEntry e,
            final OutcomeMeasureDefinition def) {
        return new OutcomeMeasureEntryResponse(
                e.getId(), def.getCode(), def.getDisplayName(),
                e.getScore(), e.getAssessmentDate(), e.getNotes(),
                e.isThresholdBreached(), e.getAlertLabel(), e.getAlertSeverity(),
                e.getRecordedByName(), e.getCreatedAt());
    }
}
