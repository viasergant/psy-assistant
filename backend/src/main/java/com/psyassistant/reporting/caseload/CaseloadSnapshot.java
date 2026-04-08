package com.psyassistant.reporting.caseload;

import com.psyassistant.common.audit.SimpleBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily snapshot of a therapist's caseload metrics.
 *
 * <p>One row per therapist per day, upserted by {@link CaseloadSnapshotJob}.
 * The unique constraint {@code uq_caseload_snapshot} prevents duplicates.
 */
@Entity
@Table(name = "caseload_snapshot")
public class CaseloadSnapshot extends SimpleBaseEntity {

    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "active_client_count", nullable = false)
    private int activeClientCount;

    @Column(name = "sessions_this_week", nullable = false)
    private int sessionsThisWeek;

    @Column(name = "sessions_this_month", nullable = false)
    private int sessionsThisMonth;

    @Column(name = "scheduled_hours_this_week", nullable = false)
    private BigDecimal scheduledHoursThisWeek;

    @Column(name = "contracted_hours_per_week")
    private BigDecimal contractedHoursPerWeek;

    @Column(name = "utilization_rate")
    private BigDecimal utilizationRate;

    /** Required by JPA. */
    protected CaseloadSnapshot() {
    }

    /**
     * Constructs a new snapshot for the given therapist.
     *
     * @param therapistProfileId  UUID of the therapist_profile record
     * @param snapshotDate        date of the snapshot
     * @param activeClientCount   number of active clients
     * @param sessionsThisWeek    sessions in the current ISO week
     * @param sessionsThisMonth   sessions in the current calendar month
     * @param scheduledHoursThisWeek scheduled hours in the current ISO week
     * @param contractedHoursPerWeek contracted hours per week (may be null)
     * @param utilizationRate     utilization rate (may be null)
     */
    public CaseloadSnapshot(
            final UUID therapistProfileId,
            final LocalDate snapshotDate,
            final int activeClientCount,
            final int sessionsThisWeek,
            final int sessionsThisMonth,
            final BigDecimal scheduledHoursThisWeek,
            final BigDecimal contractedHoursPerWeek,
            final BigDecimal utilizationRate) {
        this.therapistProfileId = therapistProfileId;
        this.snapshotDate = snapshotDate;
        this.activeClientCount = activeClientCount;
        this.sessionsThisWeek = sessionsThisWeek;
        this.sessionsThisMonth = sessionsThisMonth;
        this.scheduledHoursThisWeek = scheduledHoursThisWeek;
        this.contractedHoursPerWeek = contractedHoursPerWeek;
        this.utilizationRate = utilizationRate;
    }

    /**
     * Returns the therapist profile ID.
     *
     * @return therapist profile UUID
     */
    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    /**
     * Returns the snapshot date.
     *
     * @return snapshot date
     */
    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    /**
     * Returns the active client count.
     *
     * @return active client count
     */
    public int getActiveClientCount() {
        return activeClientCount;
    }

    /**
     * Returns the number of sessions in the current ISO week.
     *
     * @return sessions this week
     */
    public int getSessionsThisWeek() {
        return sessionsThisWeek;
    }

    /**
     * Returns the number of sessions in the current calendar month.
     *
     * @return sessions this month
     */
    public int getSessionsThisMonth() {
        return sessionsThisMonth;
    }

    /**
     * Returns the scheduled hours in the current ISO week.
     *
     * @return scheduled hours this week
     */
    public BigDecimal getScheduledHoursThisWeek() {
        return scheduledHoursThisWeek;
    }

    /**
     * Returns the contracted hours per week, or {@code null} if not set.
     *
     * @return contracted hours per week or null
     */
    public BigDecimal getContractedHoursPerWeek() {
        return contractedHoursPerWeek;
    }

    /**
     * Returns the utilization rate, or {@code null} if contracted hours not set.
     *
     * @return utilization rate or null
     */
    public BigDecimal getUtilizationRate() {
        return utilizationRate;
    }
}
