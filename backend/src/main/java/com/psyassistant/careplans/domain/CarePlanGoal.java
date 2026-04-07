package com.psyassistant.careplans.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A single treatment goal within a {@link CarePlan}.
 */
@Entity
@Table(name = "care_plan_goal")
public class CarePlanGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_plan_id", nullable = false, updatable = false)
    private CarePlan carePlan;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", nullable = false)
    private short priority = 0;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GoalStatus status = GoalStatus.PENDING;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("created_at ASC")
    private List<CarePlanIntervention> interventions = new ArrayList<>();

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("target_date ASC NULLS LAST, created_at ASC")
    private List<CarePlanMilestone> milestones = new ArrayList<>();

    // ---- constructors ----

    protected CarePlanGoal() { }

    public CarePlanGoal(final CarePlan carePlan, final String description,
                        final short priority, final LocalDate targetDate) {
        this.carePlan = carePlan;
        this.description = description;
        this.priority = priority;
        this.targetDate = targetDate;
    }

    // ---- getters / setters ----

    public CarePlan getCarePlan() {
        return carePlan;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(final short priority) {
        this.priority = priority;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(final LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(final GoalStatus status) {
        this.status = status;
    }

    public List<CarePlanIntervention> getInterventions() {
        return interventions;
    }

    public List<CarePlanMilestone> getMilestones() {
        return milestones;
    }
}
