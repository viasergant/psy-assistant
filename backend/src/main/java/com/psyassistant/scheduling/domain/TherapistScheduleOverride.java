package com.psyassistant.scheduling.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a one-off schedule exception for a specific date.
 *
 * <p>Overrides take precedence over the recurring weekly schedule. An override can either:
 * <ul>
 *     <li>Mark the entire day as unavailable ({@code isAvailable = false})</li>
 *     <li>Define custom working hours for that date ({@code isAvailable = true} with start/end times)</li>
 * </ul>
 *
 * <p>Extends {@link BaseEntity} to inherit UUID primary key and Spring Data Auditing fields.
 */
@Entity
@Table(name = "therapist_schedule_override")
public class TherapistScheduleOverride extends BaseEntity {

    /** Foreign key to the therapist profile. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** The specific date for which this override applies. */
    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    /** Whether the therapist is available on this date (false = entire day unavailable). */
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = false;

    /** Start time if custom hours are defined (null if isAvailable = false). */
    @Column(name = "start_time")
    private LocalTime startTime;

    /** End time if custom hours are defined (null if isAvailable = false). */
    @Column(name = "end_time")
    private LocalTime endTime;

    /** Optional human-readable explanation for the override. */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Default constructor for JPA.
     */
    protected TherapistScheduleOverride() {
    }

    /**
     * Creates a new override marking the entire day as unavailable.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideDate date to override
     * @param reason optional explanation
     */
    public TherapistScheduleOverride(final UUID therapistProfileId,
                                      final LocalDate overrideDate,
                                      final String reason) {
        this.therapistProfileId = therapistProfileId;
        this.overrideDate = overrideDate;
        this.isAvailable = false;
        this.reason = reason;
    }

    /**
     * Creates a new override with custom working hours for a specific date.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideDate date to override
     * @param startTime custom start time
     * @param endTime custom end time
     * @param reason optional explanation
     */
    public TherapistScheduleOverride(final UUID therapistProfileId,
                                      final LocalDate overrideDate,
                                      final LocalTime startTime,
                                      final LocalTime endTime,
                                      final String reason) {
        this.therapistProfileId = therapistProfileId;
        this.overrideDate = overrideDate;
        this.isAvailable = true;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    // Getters and setters

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(final UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public LocalDate getOverrideDate() {
        return overrideDate;
    }

    public void setOverrideDate(final LocalDate overrideDate) {
        this.overrideDate = overrideDate;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(final Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }
}
