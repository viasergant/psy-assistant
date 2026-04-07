package com.psyassistant.careplans.domain;

/** Immutable action types recorded in {@code care_plan_audit}. */
public enum AuditActionType {
    PLAN_CREATED,
    PLAN_UPDATED,
    PLAN_CLOSED,
    PLAN_ARCHIVED,
    GOAL_ADDED,
    GOAL_UPDATED,
    GOAL_STATUS_CHANGED,
    GOAL_REMOVED,
    INTERVENTION_ADDED,
    INTERVENTION_UPDATED,
    INTERVENTION_REMOVED,
    MILESTONE_ADDED,
    MILESTONE_ACHIEVED,
    MILESTONE_REMOVED
}
