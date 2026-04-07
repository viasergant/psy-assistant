package com.psyassistant.careplans.service;

import com.psyassistant.careplans.domain.AuditActionType;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanAudit;
import com.psyassistant.careplans.domain.CarePlanGoal;
import com.psyassistant.careplans.domain.CarePlanIntervention;
import com.psyassistant.careplans.domain.CarePlanMilestone;
import com.psyassistant.careplans.repository.CarePlanAuditRepository;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Records immutable audit entries for all care plan mutations.
 *
 * <p>Each method must be called within the same {@code @Transactional} boundary as the
 * mutation, so the audit entry is either committed or rolled back atomically with the domain change.
 */
@Service
public class CarePlanAuditService {

    private static final String ENTITY_PLAN = "CARE_PLAN";
    private static final String ENTITY_GOAL = "GOAL";
    private static final String ENTITY_INTERVENTION = "INTERVENTION";
    private static final String ENTITY_MILESTONE = "MILESTONE";

    private final CarePlanAuditRepository auditRepository;

    public CarePlanAuditService(final CarePlanAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void recordPlanCreated(final CarePlan plan, final UUID actorId, final String actorName) {
        save(plan.getId(), ENTITY_PLAN, plan.getId(), AuditActionType.PLAN_CREATED,
                actorId, actorName, null, null, null);
    }

    public void recordPlanUpdated(final CarePlan plan, final UUID actorId, final String actorName,
                                   final String field, final String oldVal, final String newVal) {
        save(plan.getId(), ENTITY_PLAN, plan.getId(), AuditActionType.PLAN_UPDATED,
                actorId, actorName, field, oldVal, newVal);
    }

    public void recordPlanClosed(final CarePlan plan, final UUID actorId, final String actorName) {
        save(plan.getId(), ENTITY_PLAN, plan.getId(), AuditActionType.PLAN_CLOSED,
                actorId, actorName, "status", "ACTIVE", "CLOSED");
    }

    public void recordPlanArchived(final CarePlan plan, final UUID actorId, final String actorName) {
        save(plan.getId(), ENTITY_PLAN, plan.getId(), AuditActionType.PLAN_ARCHIVED,
                actorId, actorName, "status", plan.getStatus().name(), "ARCHIVED");
    }

    public void recordGoalAdded(final CarePlanGoal goal, final UUID actorId, final String actorName) {
        save(goal.getCarePlan().getId(), ENTITY_GOAL, goal.getId(),
                AuditActionType.GOAL_ADDED, actorId, actorName, null, null, null);
    }

    public void recordGoalUpdated(final CarePlanGoal goal, final UUID actorId, final String actorName,
                                   final String field, final String oldVal, final String newVal) {
        save(goal.getCarePlan().getId(), ENTITY_GOAL, goal.getId(),
                AuditActionType.GOAL_UPDATED, actorId, actorName, field, oldVal, newVal);
    }

    public void recordGoalStatusChanged(final CarePlanGoal goal, final UUID actorId,
                                         final String actorName, final String oldStatus) {
        save(goal.getCarePlan().getId(), ENTITY_GOAL, goal.getId(),
                AuditActionType.GOAL_STATUS_CHANGED, actorId, actorName,
                "status", oldStatus, goal.getStatus().name());
    }

    public void recordGoalRemoved(final UUID carePlanId, final UUID goalId,
                                   final UUID actorId, final String actorName) {
        save(carePlanId, ENTITY_GOAL, goalId, AuditActionType.GOAL_REMOVED,
                actorId, actorName, null, null, null);
    }

    public void recordInterventionAdded(final CarePlanIntervention intervention,
                                         final UUID actorId, final String actorName) {
        save(intervention.getGoal().getCarePlan().getId(), ENTITY_INTERVENTION,
                intervention.getId(), AuditActionType.INTERVENTION_ADDED,
                actorId, actorName, null, null, null);
    }

    public void recordInterventionUpdated(final CarePlanIntervention intervention,
                                           final UUID actorId, final String actorName) {
        save(intervention.getGoal().getCarePlan().getId(), ENTITY_INTERVENTION,
                intervention.getId(), AuditActionType.INTERVENTION_UPDATED,
                actorId, actorName, null, null, null);
    }

    public void recordInterventionRemoved(final UUID carePlanId, final UUID interventionId,
                                           final UUID actorId, final String actorName) {
        save(carePlanId, ENTITY_INTERVENTION, interventionId,
                AuditActionType.INTERVENTION_REMOVED, actorId, actorName, null, null, null);
    }

    public void recordMilestoneAdded(final CarePlanMilestone milestone,
                                      final UUID actorId, final String actorName) {
        save(milestone.getGoal().getCarePlan().getId(), ENTITY_MILESTONE,
                milestone.getId(), AuditActionType.MILESTONE_ADDED,
                actorId, actorName, null, null, null);
    }

    public void recordMilestoneAchieved(final CarePlanMilestone milestone,
                                         final UUID actorId, final String actorName) {
        save(milestone.getGoal().getCarePlan().getId(), ENTITY_MILESTONE,
                milestone.getId(), AuditActionType.MILESTONE_ACHIEVED,
                actorId, actorName, "achievedAt", null, milestone.getAchievedAt().toString());
    }

    public void recordMilestoneRemoved(final UUID carePlanId, final UUID milestoneId,
                                        final UUID actorId, final String actorName) {
        save(carePlanId, ENTITY_MILESTONE, milestoneId, AuditActionType.MILESTONE_REMOVED,
                actorId, actorName, null, null, null);
    }

    // ---- helpers ----

    private void save(final UUID carePlanId, final String entityType, final UUID entityId,
                      final AuditActionType actionType, final UUID actorUserId,
                      final String actorName, final String fieldName,
                      final String oldValue, final String newValue) {
        final String requestId = MDC.get("requestId");
        final CarePlanAudit entry = new CarePlanAudit(
                carePlanId, entityType, entityId, actionType,
                actorUserId, actorName, fieldName, oldValue, newValue, requestId);
        auditRepository.save(entry);
    }
}
