package com.psyassistant.careplans.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Read-only definition of a standardised outcome measurement instrument
 * (e.g. PHQ-9, GAD-7). Seeded once via Flyway; never created by API callers.
 */
@Entity
@Table(name = "outcome_measure_definition")
public class OutcomeMeasureDefinition extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_score", nullable = false)
    private int minScore;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "alert_threshold")
    private Integer alertThreshold;

    @Column(name = "alert_label", length = 255)
    private String alertLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_severity", length = 10)
    private AlertSeverity alertSeverity;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // ---- constructors ----

    protected OutcomeMeasureDefinition() { }

    // ---- getters ----

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public String getAlertLabel() {
        return alertLabel;
    }

    public AlertSeverity getAlertSeverity() {
        return alertSeverity;
    }

    public short getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
