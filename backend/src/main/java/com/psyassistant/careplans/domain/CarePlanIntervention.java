package com.psyassistant.careplans.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A planned therapeutic intervention linked to a {@link CarePlanGoal}.
 */
@Entity
@Table(name = "care_plan_intervention")
public class CarePlanIntervention extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    private CarePlanGoal goal;

    /** Type code validated against {@code app.care-plans.intervention-types} config list. */
    @Column(name = "intervention_type", nullable = false, length = 100)
    private String interventionType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "frequency", length = 100)
    private String frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InterventionStatus status = InterventionStatus.ACTIVE;

    // ---- constructors ----

    protected CarePlanIntervention() { }

    public CarePlanIntervention(final CarePlanGoal goal, final String interventionType,
                                 final String description, final String frequency) {
        this.goal = goal;
        this.interventionType = interventionType;
        this.description = description;
        this.frequency = frequency;
    }

    // ---- getters / setters ----

    public CarePlanGoal getGoal() {
        return goal;
    }

    public String getInterventionType() {
        return interventionType;
    }

    public void setInterventionType(final String interventionType) {
        this.interventionType = interventionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(final String frequency) {
        this.frequency = frequency;
    }

    public InterventionStatus getStatus() {
        return status;
    }

    public void setStatus(final InterventionStatus status) {
        this.status = status;
    }
}
