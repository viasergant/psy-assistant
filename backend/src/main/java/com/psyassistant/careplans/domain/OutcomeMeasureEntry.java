package com.psyassistant.careplans.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Append-only record of a single outcome measure assessment (score + date).
 *
 * <p>Threshold evaluation is denormalised at write time: {@code thresholdBreached},
 * {@code alertLabel}, and {@code alertSeverity} are copied from the definition so that
 * historical entries remain accurate even if the definition thresholds change later.
 */
@Entity
@Table(name = "outcome_measure_entry")
@EntityListeners(AuditingEntityListener.class)
public class OutcomeMeasureEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_plan_id", nullable = false, updatable = false)
    private UUID carePlanId;

    @Column(name = "measure_definition_id", nullable = false, updatable = false)
    private UUID measureDefinitionId;

    @Column(name = "score", nullable = false, updatable = false)
    private int score;

    @Column(name = "assessment_date", nullable = false, updatable = false)
    private LocalDate assessmentDate;

    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String notes;

    @Column(name = "recorded_by_user_id", nullable = false, updatable = false)
    private UUID recordedByUserId;

    @Column(name = "recorded_by_name", length = 255, updatable = false)
    private String recordedByName;

    @Column(name = "threshold_breached", nullable = false, updatable = false)
    private boolean thresholdBreached;

    @Column(name = "alert_label", length = 255, updatable = false)
    private String alertLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_severity", length = 10, updatable = false)
    private AlertSeverity alertSeverity;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    // ---- constructors ----

    protected OutcomeMeasureEntry() { }

    public OutcomeMeasureEntry(final UUID carePlanId,
                                final UUID measureDefinitionId,
                                final int score,
                                final LocalDate assessmentDate,
                                final String notes,
                                final UUID recordedByUserId,
                                final String recordedByName,
                                final boolean thresholdBreached,
                                final String alertLabel,
                                final AlertSeverity alertSeverity) {
        this.carePlanId = carePlanId;
        this.measureDefinitionId = measureDefinitionId;
        this.score = score;
        this.assessmentDate = assessmentDate;
        this.notes = notes;
        this.recordedByUserId = recordedByUserId;
        this.recordedByName = recordedByName;
        this.thresholdBreached = thresholdBreached;
        this.alertLabel = alertLabel;
        this.alertSeverity = alertSeverity;
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public UUID getCarePlanId() {
        return carePlanId;
    }

    public UUID getMeasureDefinitionId() {
        return measureDefinitionId;
    }

    public int getScore() {
        return score;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getRecordedByUserId() {
        return recordedByUserId;
    }

    public String getRecordedByName() {
        return recordedByName;
    }

    public boolean isThresholdBreached() {
        return thresholdBreached;
    }

    public String getAlertLabel() {
        return alertLabel;
    }

    public AlertSeverity getAlertSeverity() {
        return alertSeverity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
