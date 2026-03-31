package com.psyassistant.scheduling.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a therapist's recurring weekly working hours for a specific day of the week.
 *
 * <p>Extends {@link BaseEntity} to inherit UUID primary key and Spring Data Auditing
 * fields ({@code createdAt}, {@code updatedAt}, {@code createdBy}).
 *
 * <p>Each row defines availability for one day of the week (Monday=1 through Sunday=7).
 * A therapist may have multiple entries for the same day if they work split shifts.
 */
@Entity
@Table(name = "therapist_recurring_schedule")
public class TherapistRecurringSchedule extends BaseEntity {

    /** Foreign key to the therapist profile. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** Day of week using ISO-8601 convention: 1=Monday, 2=Tuesday, ..., 7=Sunday. */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    /** Start time of availability window (30-minute increment constraint enforced at DB level). */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** End time of availability window (must be after start_time, 30-minute increment). */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** IANA timezone identifier (e.g., "America/New_York", "Europe/London"). */
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    /**
     * Default constructor for JPA.
     */
    protected TherapistRecurringSchedule() {
    }

    /**
     * Creates a new recurring schedule entry.
     *
     * @param therapistProfileId therapist profile UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @param startTime start time of availability
     * @param endTime end time of availability
     * @param timezone IANA timezone identifier
     */
    public TherapistRecurringSchedule(final UUID therapistProfileId,
                                       final Integer dayOfWeek,
                                       final LocalTime startTime,
                                       final LocalTime endTime,
                                       final String timezone) {
        this.therapistProfileId = therapistProfileId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone;
    }

    // Getters and setters

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(final UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(final Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }
}
