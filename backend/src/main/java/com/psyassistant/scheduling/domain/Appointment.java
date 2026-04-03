package com.psyassistant.scheduling.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents a therapy appointment booking.
 *
 * <p>Appointments are the core scheduling entity, linking a therapist, client, and session type
 * to a specific time slot. Conflict detection prevents double booking using PostgreSQL tstzrange
 * with GIST indexing.
 *
 * <p>Key features:
 * <ul>
 *     <li><strong>Optimistic locking</strong>: {@code @Version} prevents lost updates in concurrent modifications</li>
 *     <li><strong>Conflict detection</strong>: GIST index enables O(log n) overlap queries</li>
 *     <li><strong>Audit trail</strong>: All changes tracked in {@link AppointmentAudit}</li>
 *     <li><strong>Lifecycle management</strong>: Status transitions from SCHEDULED → CONFIRMED → COMPLETED</li>
 * </ul>
 *
 * <p>Extends {@link BaseEntity} to inherit UUID primary key and Spring Data Auditing.
 */
@Entity
@Table(name = "appointment")
public class Appointment extends BaseEntity {

    /** Foreign key to therapist profile. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** Foreign key to client. */
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /** Session type (e.g., in-person, online, intake). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    // ========== Scheduling Fields ==========

    /** Appointment start time with timezone information. */
    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    /** Duration in minutes (must be multiple of 15). */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** IANA timezone identifier (e.g., "America/New_York", "Europe/London"). */
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    // ========== Status and Tracking ==========

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    /** TRUE if created with explicit conflict override permission. */
    @Column(name = "is_conflict_override", nullable = false)
    private Boolean isConflictOverride = false;

    // ========== Cancellation Fields ==========

    /** Who initiated the cancellation (null if not cancelled). */
    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_type", length = 50)
    private CancellationType cancellationType;

    /** Human-readable reason for cancellation (null if not cancelled). */
    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    /** When the appointment was cancelled (null if not cancelled). */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** User who cancelled the appointment (null if not cancelled). */
    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    // ========== Reschedule Tracking ==========

    /** Why the appointment was rescheduled (null if never rescheduled). */
    @Column(name = "reschedule_reason", length = 1000)
    private String rescheduleReason;

    /** Original start time before first reschedule (null if never rescheduled). */
    @Column(name = "original_start_time")
    private ZonedDateTime originalStartTime;

    /** When the appointment was last rescheduled (null if never rescheduled). */
    @Column(name = "rescheduled_at")
    private Instant rescheduledAt;

    /** User who last rescheduled the appointment (null if never rescheduled). */
    @Column(name = "rescheduled_by")
    private UUID rescheduledBy;

    // ========== Notes and Metadata ==========

    /** Free-form notes about the appointment (session prep, special requirements, etc.). */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========== Recurring Series Fields (PA-33) ==========

    /**
     * Parent series for this occurrence.
     *
     * <p>Null for one-off (non-recurring) appointments.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private AppointmentSeries series;

    /**
     * Zero-based position of this occurrence within its series.
     *
     * <p>Null for one-off appointments.
     */
    @Column(name = "recurrence_index")
    private Integer recurrenceIndex;

    /**
     * True if this occurrence was individually edited after series creation.
     *
     * <p>Triggers a "modified" visual indicator in the calendar UI.
     */
    @Column(name = "is_modified", nullable = false)
    private boolean isModified = false;

    // ========== Optimistic Locking ==========

    /**
     * Version number for optimistic locking.
     *
     * <p>Incremented automatically by JPA on every update. Concurrent modification detection
     * throws {@link jakarta.persistence.OptimisticLockException} if version mismatch occurs.
     * Service layer should retry with {@link org.springframework.retry.annotation.Retryable}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Default constructor for JPA.
     */
    protected Appointment() {
    }

    /**
     * Creates a new appointment.
     *
     * @param therapistProfileId therapist UUID
     * @param clientId client UUID
     * @param sessionType session type entity
     * @param startTime appointment start time with timezone
     * @param durationMinutes duration in minutes (multiple of 15)
     * @param timezone IANA timezone identifier
     */
    public Appointment(final UUID therapistProfileId,
                        final UUID clientId,
                        final SessionType sessionType,
                        final ZonedDateTime startTime,
                        final Integer durationMinutes,
                        final String timezone) {
        this.therapistProfileId = therapistProfileId;
        this.clientId = clientId;
        this.sessionType = sessionType;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.timezone = timezone;
        this.status = AppointmentStatus.SCHEDULED;
        this.isConflictOverride = false;
    }

    // ========== Business Methods ==========

    /**
     * Calculated end time based on start time and duration.
     *
     * @return end time (start_time + duration_minutes)
     */
    public ZonedDateTime getEndTime() {
        return startTime != null && durationMinutes != null
                ? startTime.plusMinutes(durationMinutes)
                : null;
    }

    /**
     * Marks the appointment as cancelled.
     *
     * @param type who initiated the cancellation
     * @param reason human-readable explanation
     * @param cancelledBy user UUID who performed the cancellation
     */
    public void cancel(final CancellationType type, final String reason, final UUID cancelledBy) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationType = type;
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
        this.cancelledBy = cancelledBy;
    }

    /**
     * Reschedules the appointment to a new time.
     *
     * @param newStartTime new start time
     * @param reason why the appointment is being rescheduled
     * @param rescheduledBy user UUID who performed the reschedule
     */
    public void reschedule(final ZonedDateTime newStartTime, final String reason, final UUID rescheduledBy) {
        if (this.originalStartTime == null) {
            // First reschedule - preserve original time
            this.originalStartTime = this.startTime;
        }
        this.startTime = newStartTime;
        this.rescheduleReason = reason;
        this.rescheduledAt = Instant.now();
        this.rescheduledBy = rescheduledBy;
    }

    // ========== Getters and Setters ==========

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(final UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(final UUID clientId) {
        this.clientId = clientId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(final SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(final Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(final AppointmentStatus status) {
        this.status = status;
    }

    public Boolean getIsConflictOverride() {
        return isConflictOverride;
    }

    public void setIsConflictOverride(final Boolean isConflictOverride) {
        this.isConflictOverride = isConflictOverride;
    }

    public CancellationType getCancellationType() {
        return cancellationType;
    }

    public void setCancellationType(final CancellationType cancellationType) {
        this.cancellationType = cancellationType;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(final String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(final Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(final UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getRescheduleReason() {
        return rescheduleReason;
    }

    public void setRescheduleReason(final String rescheduleReason) {
        this.rescheduleReason = rescheduleReason;
    }

    public ZonedDateTime getOriginalStartTime() {
        return originalStartTime;
    }

    public void setOriginalStartTime(final ZonedDateTime originalStartTime) {
        this.originalStartTime = originalStartTime;
    }

    public Instant getRescheduledAt() {
        return rescheduledAt;
    }

    public void setRescheduledAt(final Instant rescheduledAt) {
        this.rescheduledAt = rescheduledAt;
    }

    public UUID getRescheduledBy() {
        return rescheduledBy;
    }

    public void setRescheduledBy(final UUID rescheduledBy) {
        this.rescheduledBy = rescheduledBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    // ========== Recurring Series Getters/Setters (PA-33) ==========

    public AppointmentSeries getSeries() {
        return series;
    }

    public void setSeries(final AppointmentSeries series) {
        this.series = series;
    }

    public Integer getRecurrenceIndex() {
        return recurrenceIndex;
    }

    public void setRecurrenceIndex(final Integer recurrenceIndex) {
        this.recurrenceIndex = recurrenceIndex;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(final boolean modified) {
        this.isModified = modified;
    }
}
