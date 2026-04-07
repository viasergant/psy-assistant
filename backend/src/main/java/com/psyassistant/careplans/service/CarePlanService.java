package com.psyassistant.careplans.service;

import com.psyassistant.careplans.config.CarePlanProperties;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanGoal;
import com.psyassistant.careplans.domain.CarePlanIntervention;
import com.psyassistant.careplans.domain.CarePlanMilestone;
import com.psyassistant.careplans.domain.CarePlanStatus;
import com.psyassistant.careplans.dto.CarePlanAuditResponse;
import com.psyassistant.careplans.dto.CarePlanDetailResponse;
import com.psyassistant.careplans.dto.CarePlanSummaryResponse;
import com.psyassistant.careplans.dto.CreateCarePlanRequest;
import com.psyassistant.careplans.dto.CreateGoalRequest;
import com.psyassistant.careplans.dto.CreateInterventionRequest;
import com.psyassistant.careplans.dto.CreateMilestoneRequest;
import com.psyassistant.careplans.dto.GoalResponse;
import com.psyassistant.careplans.dto.InterventionResponse;
import com.psyassistant.careplans.dto.MilestoneResponse;
import com.psyassistant.careplans.dto.UpdateCarePlanRequest;
import com.psyassistant.careplans.dto.UpdateGoalStatusRequest;
import com.psyassistant.careplans.exception.CarePlanNotActiveException;
import com.psyassistant.careplans.exception.MaxActivePlansExceededException;
import com.psyassistant.careplans.repository.CarePlanAuditRepository;
import com.psyassistant.careplans.repository.CarePlanRepository;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service for care plan lifecycle management.
 *
 * <p>RBAC rules:
 * <ul>
 *   <li>READ operations: {@code READ_CARE_PLANS} (THERAPIST, SUPERVISOR, SYS_ADMIN, RAS)</li>
 *   <li>WRITE operations: {@code MANAGE_CARE_PLANS} (THERAPIST, SYS_ADMIN)</li>
 * </ul>
 *
 * <p>Assignment check (service layer, not HTTP layer):
 * Callers without {@code READ_CLIENTS_ALL} may only access plans where
 * {@code plan.therapistId == actorId}.
 */
@Service
public class CarePlanService {

    private static final Logger LOG = LoggerFactory.getLogger(CarePlanService.class);

    private final CarePlanRepository carePlanRepository;
    private final CarePlanAuditRepository carePlanAuditRepository;
    private final CarePlanAuditService auditService;
    private final CarePlanProperties properties;
    private final UserRepository userRepository;

    public CarePlanService(
            final CarePlanRepository carePlanRepository,
            final CarePlanAuditRepository carePlanAuditRepository,
            final CarePlanAuditService auditService,
            final CarePlanProperties properties,
            final UserRepository userRepository) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanAuditRepository = carePlanAuditRepository;
        this.auditService = auditService;
        this.properties = properties;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // Care Plan CRUD
    // =========================================================================

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public List<CarePlanSummaryResponse> listByClient(
            final UUID clientId,
            final CarePlanStatus statusFilter,
            final UUID actorId,
            final boolean hasReadAll) {

        final List<CarePlan> plans = statusFilter != null
                ? carePlanRepository.findByClientIdAndStatusOrderByCreatedAtDesc(clientId, statusFilter)
                : carePlanRepository.findByClientIdOrderByCreatedAtDesc(clientId);

        return plans.stream()
                .filter(p -> hasReadAll || p.getTherapistId().equals(actorId))
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public CarePlanDetailResponse create(
            final UUID clientId,
            final CreateCarePlanRequest request,
            final UUID actorId,
            final String actorName) {

        final long activeCount = carePlanRepository.countActiveByClientId(clientId);
        if (activeCount >= properties.maxActivePerClient()) {
            throw new MaxActivePlansExceededException(
                    "Client already has " + activeCount + " active care plan(s). Maximum is "
                    + properties.maxActivePerClient() + ".");
        }

        final CarePlan plan = new CarePlan(clientId, actorId, request.title(), request.description());
        addGoalsFromRequests(plan, request.goals());

        final CarePlan saved = carePlanRepository.save(plan);
        auditService.recordPlanCreated(saved, actorId, actorName);

        LOG.info("Care plan created: id={}, client={}, therapist={}", saved.getId(), clientId, actorId);
        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public CarePlanDetailResponse getDetail(
            final UUID planId,
            final UUID actorId,
            final boolean hasReadAll) {

        final CarePlan plan = loadPlan(planId);
        verifyAccess(plan, actorId, hasReadAll);
        return toDetail(plan);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public CarePlanDetailResponse update(
            final UUID planId,
            final UpdateCarePlanRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        if (request.title() != null && !request.title().equals(plan.getTitle())) {
            auditService.recordPlanUpdated(plan, actorId, actorName, "title",
                    plan.getTitle(), request.title());
            plan.setTitle(request.title());
        }
        if (request.description() != null && !request.description().equals(plan.getDescription())) {
            auditService.recordPlanUpdated(plan, actorId, actorName, "description",
                    plan.getDescription(), request.description());
            plan.setDescription(request.description());
        }

        return toDetail(carePlanRepository.save(plan));
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public void close(final UUID planId, final UUID actorId, final String actorName) {
        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        plan.close(actorId);
        carePlanRepository.save(plan);
        auditService.recordPlanClosed(plan, actorId, actorName);

        LOG.info("Care plan closed: id={}, by={}", planId, actorId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public void archive(final UUID planId, final UUID actorId, final String actorName) {
        final CarePlan plan = loadPlan(planId);
        verifyOwnership(plan, actorId);

        auditService.recordPlanArchived(plan, actorId, actorName);
        plan.archive();
        carePlanRepository.save(plan);

        LOG.info("Care plan archived: id={}, by={}", planId, actorId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public Page<CarePlanAuditResponse> getAuditLog(
            final UUID planId,
            final UUID actorId,
            final boolean hasReadAll,
            final Pageable pageable) {

        final CarePlan plan = loadPlan(planId);
        verifyAccess(plan, actorId, hasReadAll);

        return carePlanAuditRepository
                .findByCarePlanIdOrderByActionTimestampDesc(planId, pageable)
                .map(a -> new CarePlanAuditResponse(
                        a.getId(), a.getCarePlanId(), a.getEntityType(), a.getEntityId(),
                        a.getActionType(), a.getActorUserId(), a.getActorName(),
                        a.getActionTimestamp(), a.getFieldName(), a.getOldValue(), a.getNewValue()));
    }

    // =========================================================================
    // Goal CRUD
    // =========================================================================

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public GoalResponse addGoal(
            final UUID planId,
            final CreateGoalRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = new CarePlanGoal(
                plan, request.description(), request.priority(), request.targetDate());

        if (request.interventions() != null) {
            for (final CreateInterventionRequest ir : request.interventions()) {
                validateInterventionType(ir.interventionType());
                goal.getInterventions().add(new CarePlanIntervention(
                        goal, ir.interventionType(), ir.description(), ir.frequency()));
            }
        }
        if (request.milestones() != null) {
            for (final CreateMilestoneRequest mr : request.milestones()) {
                goal.getMilestones().add(new CarePlanMilestone(goal, mr.description(), mr.targetDate()));
            }
        }

        plan.getGoals().add(goal);
        carePlanRepository.save(plan);

        auditService.recordGoalAdded(goal, actorId, actorName);
        return toGoalResponse(goal);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public GoalResponse updateGoal(
            final UUID planId,
            final UUID goalId,
            final CreateGoalRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);

        if (!request.description().equals(goal.getDescription())) {
            auditService.recordGoalUpdated(goal, actorId, actorName, "description",
                    goal.getDescription(), request.description());
            goal.setDescription(request.description());
        }
        goal.setPriority(request.priority());
        goal.setTargetDate(request.targetDate());
        carePlanRepository.save(plan);

        return toGoalResponse(goal);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public GoalResponse updateGoalStatus(
            final UUID planId,
            final UUID goalId,
            final UpdateGoalStatusRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);
        final String oldStatus = goal.getStatus().name();
        goal.setStatus(request.status());
        carePlanRepository.save(plan);

        auditService.recordGoalStatusChanged(goal, actorId, actorName, oldStatus);
        return toGoalResponse(goal);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public void removeGoal(
            final UUID planId,
            final UUID goalId,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        plan.getGoals().removeIf(g -> g.getId().equals(goalId));
        carePlanRepository.save(plan);
        auditService.recordGoalRemoved(planId, goalId, actorId, actorName);
    }

    // =========================================================================
    // Intervention CRUD
    // =========================================================================

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public InterventionResponse addIntervention(
            final UUID planId,
            final UUID goalId,
            final CreateInterventionRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);
        validateInterventionType(request.interventionType());

        final CarePlanGoal goal = findGoal(plan, goalId);
        final CarePlanIntervention intervention = new CarePlanIntervention(
                goal, request.interventionType(), request.description(), request.frequency());
        goal.getInterventions().add(intervention);
        carePlanRepository.save(plan);

        auditService.recordInterventionAdded(intervention, actorId, actorName);
        return toInterventionResponse(intervention);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public InterventionResponse updateIntervention(
            final UUID planId,
            final UUID goalId,
            final UUID interventionId,
            final CreateInterventionRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);
        validateInterventionType(request.interventionType());

        final CarePlanGoal goal = findGoal(plan, goalId);
        final CarePlanIntervention intervention = findIntervention(goal, interventionId);
        intervention.setInterventionType(request.interventionType());
        intervention.setDescription(request.description());
        intervention.setFrequency(request.frequency());
        carePlanRepository.save(plan);

        auditService.recordInterventionUpdated(intervention, actorId, actorName);
        return toInterventionResponse(intervention);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public void removeIntervention(
            final UUID planId,
            final UUID goalId,
            final UUID interventionId,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);
        goal.getInterventions().removeIf(i -> i.getId().equals(interventionId));
        carePlanRepository.save(plan);
        auditService.recordInterventionRemoved(planId, interventionId, actorId, actorName);
    }

    // =========================================================================
    // Milestone CRUD
    // =========================================================================

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public MilestoneResponse addMilestone(
            final UUID planId,
            final UUID goalId,
            final CreateMilestoneRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);
        final CarePlanMilestone milestone = new CarePlanMilestone(
                goal, request.description(), request.targetDate());
        goal.getMilestones().add(milestone);
        carePlanRepository.save(plan);

        auditService.recordMilestoneAdded(milestone, actorId, actorName);
        return toMilestoneResponse(milestone);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public MilestoneResponse updateMilestone(
            final UUID planId,
            final UUID goalId,
            final UUID milestoneId,
            final CreateMilestoneRequest request,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);
        final CarePlanMilestone milestone = findMilestone(goal, milestoneId);
        milestone.setDescription(request.description());
        milestone.setTargetDate(request.targetDate());
        carePlanRepository.save(plan);

        return toMilestoneResponse(milestone);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public MilestoneResponse achieveMilestone(
            final UUID planId,
            final UUID goalId,
            final UUID milestoneId,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);
        final CarePlanMilestone milestone = findMilestone(goal, milestoneId);
        milestone.markAchieved(actorId);
        carePlanRepository.save(plan);

        auditService.recordMilestoneAchieved(milestone, actorId, actorName);
        return toMilestoneResponse(milestone);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public void removeMilestone(
            final UUID planId,
            final UUID goalId,
            final UUID milestoneId,
            final UUID actorId,
            final String actorName) {

        final CarePlan plan = loadPlan(planId);
        requireActive(plan);
        verifyOwnership(plan, actorId);

        final CarePlanGoal goal = findGoal(plan, goalId);
        goal.getMilestones().removeIf(m -> m.getId().equals(milestoneId));
        carePlanRepository.save(plan);
        auditService.recordMilestoneRemoved(planId, milestoneId, actorId, actorName);
    }

    // =========================================================================
    // Config helper
    // =========================================================================

    public List<String> getInterventionTypes() {
        return properties.interventionTypes();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private CarePlan loadPlan(final UUID planId) {
        return carePlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Care plan not found: " + planId));
    }

    private void requireActive(final CarePlan plan) {
        if (!plan.isActive()) {
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

    private void validateInterventionType(final String type) {
        if (!properties.interventionTypes().contains(type)) {
            throw new IllegalArgumentException(
                    "Unknown intervention type: '" + type + "'. Allowed: " + properties.interventionTypes());
        }
    }

    private CarePlanGoal findGoal(final CarePlan plan, final UUID goalId) {
        return plan.getGoals().stream()
                .filter(g -> g.getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + goalId));
    }

    private CarePlanIntervention findIntervention(final CarePlanGoal goal, final UUID interventionId) {
        return goal.getInterventions().stream()
                .filter(i -> i.getId().equals(interventionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Intervention not found: " + interventionId));
    }

    private CarePlanMilestone findMilestone(final CarePlanGoal goal, final UUID milestoneId) {
        return goal.getMilestones().stream()
                .filter(m -> m.getId().equals(milestoneId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Milestone not found: " + milestoneId));
    }

    private void addGoalsFromRequests(final CarePlan plan, final List<CreateGoalRequest> goalRequests) {
        for (final CreateGoalRequest req : goalRequests) {
            final CarePlanGoal goal = new CarePlanGoal(
                    plan, req.description(), req.priority(), req.targetDate());

            if (req.interventions() != null) {
                for (final CreateInterventionRequest ir : req.interventions()) {
                    validateInterventionType(ir.interventionType());
                    goal.getInterventions().add(new CarePlanIntervention(
                            goal, ir.interventionType(), ir.description(), ir.frequency()));
                }
            }
            if (req.milestones() != null) {
                for (final CreateMilestoneRequest mr : req.milestones()) {
                    goal.getMilestones().add(new CarePlanMilestone(
                            goal, mr.description(), mr.targetDate()));
                }
            }
            plan.getGoals().add(goal);
        }
    }

    // =========================================================================
    // Mappers
    // =========================================================================

    private CarePlanSummaryResponse toSummary(final CarePlan plan) {
        return new CarePlanSummaryResponse(
                plan.getId(), plan.getClientId(), plan.getTherapistId(),
                plan.getTitle(), plan.getDescription(), plan.getStatus(),
                plan.getGoals().size(), plan.getCreatedAt(), plan.getUpdatedAt());
    }

    private CarePlanDetailResponse toDetail(final CarePlan plan) {
        final List<GoalResponse> goals = plan.getGoals().stream()
                .map(this::toGoalResponse)
                .toList();
        return new CarePlanDetailResponse(
                plan.getId(), plan.getClientId(), plan.getTherapistId(),
                plan.getTitle(), plan.getDescription(), plan.getStatus(),
                plan.getClosedAt(), plan.getClosedByUserId(), plan.getArchivedAt(),
                plan.getCreatedAt(), plan.getUpdatedAt(), plan.getCreatedBy(), goals);
    }

    private GoalResponse toGoalResponse(final CarePlanGoal goal) {
        final List<InterventionResponse> interventions = goal.getInterventions().stream()
                .map(this::toInterventionResponse)
                .toList();
        final List<MilestoneResponse> milestones = goal.getMilestones().stream()
                .map(this::toMilestoneResponse)
                .toList();
        return new GoalResponse(
                goal.getId(), goal.getDescription(), goal.getPriority(),
                goal.getTargetDate(), goal.getStatus(),
                goal.getCreatedAt(), goal.getUpdatedAt(),
                interventions, milestones);
    }

    private InterventionResponse toInterventionResponse(final CarePlanIntervention intervention) {
        return new InterventionResponse(
                intervention.getId(), intervention.getInterventionType(),
                intervention.getDescription(), intervention.getFrequency(),
                intervention.getStatus(), intervention.getCreatedAt(), intervention.getUpdatedAt());
    }

    private MilestoneResponse toMilestoneResponse(final CarePlanMilestone milestone) {
        return new MilestoneResponse(
                milestone.getId(), milestone.getDescription(), milestone.getTargetDate(),
                milestone.getAchievedAt(), milestone.getAchievedByUserId(),
                milestone.getCreatedAt(), milestone.getUpdatedAt());
    }
}
