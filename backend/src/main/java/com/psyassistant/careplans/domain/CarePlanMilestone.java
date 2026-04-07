package com.psyassistant.careplans.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A measurable milestone within a {@link CarePlanGoal}.
 */
@Entity
@Table(name = "care_plan_milestone")
public class CarePlanMilestone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    private CarePlanGoal goal;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "achieved_at")
    private Instant achievedAt;

    @Column(name = "achieved_by_user_id")
    private UUID achievedByUserId;

    // ---- constructors ----

    protected CarePlanMilestone() { }

    public CarePlanMilestone(final CarePlanGoal goal, final String description,
                              final LocalDate targetDate) {
        this.goal = goal;
        this.description = description;
        this.targetDate = targetDate;
    }

    // ---- business methods ----

    public void markAchieved(final UUID actorId) {
        this.achievedAt = Instant.now();
        this.achievedByUserId = actorId;
    }

    public boolean isAchieved() {
        return this.achievedAt != null;
    }

    // ---- getters / setters ----

    public CarePlanGoal getGoal() {
        return goal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(final LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Instant getAchievedAt() {
        return achievedAt;
    }

    public UUID getAchievedByUserId() {
        return achievedByUserId;
    }
}
