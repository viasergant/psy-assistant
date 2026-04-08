package com.psyassistant.careplans.service;

import com.psyassistant.careplans.domain.AuditActionType;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanAudit;
import com.psyassistant.careplans.domain.CarePlanGoal;
import com.psyassistant.careplans.domain.CarePlanStatus;
import com.psyassistant.careplans.domain.GoalProgressNote;
import com.psyassistant.careplans.dto.GoalProgressHistoryResponse;
import com.psyassistant.careplans.dto.GoalProgressNoteResponse;
import com.psyassistant.careplans.dto.StatusChangeEvent;
import com.psyassistant.careplans.exception.CarePlanNotActiveException;
import com.psyassistant.careplans.repository.CarePlanAuditRepository;
import com.psyassistant.careplans.repository.CarePlanRepository;
import com.psyassistant.careplans.repository.GoalProgressNoteRepository;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages goal progress notes and progress history.
 *
 * <p>Progress notes are immutable once created. Visibility policy:
 * therapists see only their own notes; principals with {@code READ_CLIENTS_ALL}
 * (supervisor, admin) see all notes for the goal.
 */
@Service
public class GoalProgressService {

    private static final Logger LOG = LoggerFactory.getLogger(GoalProgressService.class);

    private final CarePlanRepository carePlanRepository;
    private final GoalProgressNoteRepository noteRepository;
    private final CarePlanAuditRepository auditRepository;
    private final CarePlanAuditService auditService;
    private final UserRepository userRepository;

    public GoalProgressService(
            final CarePlanRepository carePlanRepository,
            final GoalProgressNoteRepository noteRepository,
            final CarePlanAuditRepository auditRepository,
            final CarePlanAuditService auditService,
            final UserRepository userRepository) {
        this.carePlanRepository = carePlanRepository;
        this.noteRepository = noteRepository;
        this.auditRepository = auditRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Progress notes
    // -------------------------------------------------------------------------

    @Transactional
    public GoalProgressNoteResponse addProgressNote(
            final UUID planId,
            final UUID goalId,
            final String noteText,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);
        findGoal(plan, goalId); // validate goal belongs to plan

        final GoalProgressNote note = new GoalProgressNote(
                goalId, planId, noteText, actorId, actorName);
        noteRepository.save(note);

        auditService.recordGoalProgressNoteAdded(planId, goalId, actorId, actorName);

        LOG.info("Progress note added: goalId={}, author={}", goalId, actorId);
        return toResponse(note);
    }

    @Transactional(readOnly = true)
    public List<GoalProgressNoteResponse> getProgressNotes(
            final UUID planId,
            final UUID goalId,
            final UUID actorId,
            final boolean hasReadAll) {

        final CarePlan plan = loadPlan(planId);
        verifyAccess(plan, actorId, hasReadAll);
        findGoal(plan, goalId); // validate goal belongs to plan

        final List<GoalProgressNote> notes = hasReadAll
                ? noteRepository.findByGoalIdOrderByCreatedAtDesc(goalId)
                : noteRepository.findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(goalId, actorId);

        return notes.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoalProgressHistoryResponse getProgressHistory(
            final UUID planId,
            final UUID goalId,
            final UUID actorId,
            final boolean hasReadAll) {

        final CarePlan plan = loadPlan(planId);
        verifyAccess(plan, actorId, hasReadAll);
        findGoal(plan, goalId); // validate goal belongs to plan

        // Status change events from audit log
        final List<CarePlanAudit> allAudit =
                auditRepository.findByCarePlanIdOrderByActionTimestampDesc(planId);

        final List<StatusChangeEvent> statusHistory = allAudit.stream()
                .filter(a -> AuditActionType.GOAL_STATUS_CHANGED == a.getActionType())
                .filter(a -> goalId.equals(a.getEntityId()))
                .map(a -> new StatusChangeEvent(
                        a.getActionTimestamp(), a.getOldValue(),
                        a.getNewValue(), a.getActorName()))
                .toList();

        // Progress notes (same visibility rules)
        final List<GoalProgressNote> notes = hasReadAll
                ? noteRepository.findByGoalIdOrderByCreatedAtDesc(goalId)
                : noteRepository.findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(goalId, actorId);

        final List<GoalProgressNoteResponse> noteResponses =
                notes.stream().map(this::toResponse).toList();

        return new GoalProgressHistoryResponse(goalId, statusHistory, noteResponses);
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

    private CarePlanGoal findGoal(final CarePlan plan, final UUID goalId) {
        return plan.getGoals().stream()
                .filter(g -> g.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + goalId));
    }

    private GoalProgressNoteResponse toResponse(final GoalProgressNote note) {
        return new GoalProgressNoteResponse(
                note.getId(), note.getGoalId(), note.getNoteText(),
                note.getAuthorName(), note.getCreatedAt());
    }
}
