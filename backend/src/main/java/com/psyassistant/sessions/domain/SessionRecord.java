package com.psyassistant.sessions.domain;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.scheduling.domain.SessionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a clinical session record linked to an appointment.
 *
 * <p>Session records are created automatically when appointments transition to terminal
 * or active states (COMPLETED, IN_PROGRESS), or manually when a therapist explicitly
 * starts a session. They serve as the foundation for all session documentation workflows.
 *
 * <p>Key features:
 * <ul>
 *     <li><strong>Immutable context</strong>: Client, therapist, date, time, type, and duration
 *         fields are copied from the appointment and never changed</li>
 *     <li><strong>One-to-one mapping</strong>: Each appointment can have at most one session record
 *         (enforced by unique constraint on appointment_id)</li>
 *     <li><strong>Lifecycle tracking</strong>: Status field tracks session progression
 *         (PENDING → IN_PROGRESS → COMPLETED)</li>
 *     <li><strong>Audit trail</strong>: Inherits automatic audit fields from {@link BaseEntity}</li>
 * </ul>
 *
 * <p>Extends {@link BaseEntity} to inherit UUID primary key and Spring Data Auditing.
 */
@Entity
@Table(
    name = "session_record",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_session_record_appointment",
        columnNames = "appointment_id"
    )
)
public class SessionRecord extends BaseEntity {

    /**
     * Foreign key to the appointment that triggered this session record.
     * Must be unique across all session records.
     */
    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    /** Foreign key to client. Immutable snapshot from appointment. */
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    /** Foreign key to therapist. Immutable snapshot from appointment. */
    @Column(name = "therapist_id", nullable = false, updatable = false)
    private UUID therapistId;

    /** Session date. Immutable snapshot from appointment. */
    @Column(name = "session_date", nullable = false, updatable = false)
    private LocalDate sessionDate;

    /** Scheduled start time. Immutable snapshot from appointment. */
    @Column(name = "scheduled_start_time", nullable = false, updatable = false)
    private LocalTime scheduledStartTime;

    /** Session type (e.g., in-person, online, intake). Immutable snapshot from appointment. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_type_id", nullable = false, updatable = false)
    private SessionType sessionType;

    /** Planned duration. Immutable snapshot from appointment. */
    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "planned_duration", nullable = false, updatable = false, columnDefinition = "interval")
    private Duration plannedDuration;

    // ========== Mutable Status ==========

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SessionStatus status = SessionStatus.PENDING;

    /** Reason for cancellation (null unless status is CANCELLED). */
    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    /**
     * Default constructor for JPA.
     */
    protected SessionRecord() {
    }

    /**
     * Creates a new session record from an appointment.
     *
     * @param appointmentId appointment UUID
     * @param clientId client UUID
     * @param therapistId therapist UUID
     * @param sessionDate session date
     * @param scheduledStartTime scheduled start time
     * @param sessionType session type entity
     * @param plannedDuration planned duration
     * @param status initial status
     */
    public SessionRecord(final UUID appointmentId,
                          final UUID clientId,
                          final UUID therapistId,
                          final LocalDate sessionDate,
                          final LocalTime scheduledStartTime,
                          final SessionType sessionType,
                          final Duration plannedDuration,
                          final SessionStatus status) {
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.therapistId = therapistId;
        this.sessionDate = sessionDate;
        this.scheduledStartTime = scheduledStartTime;
        this.sessionType = sessionType;
        this.plannedDuration = plannedDuration;
        this.status = status;
    }

    // ========== Business Methods ==========

    /**
     * Marks the session as cancelled with a reason.
     *
     * @param reason human-readable explanation
     */
    public void cancel(final String reason) {
        this.status = SessionStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    /**
     * Updates the session status.
     *
     * @param newStatus new status
     */
    public void updateStatus(final SessionStatus newStatus) {
        this.status = newStatus;
    }

    // ========== Getters and Setters ==========

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(final UUID appointmentId) {
        this.appointmentId = appointmentId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public LocalTime getScheduledStartTime() {
        return scheduledStartTime;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public Duration getPlannedDuration() {
        return plannedDuration;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(final SessionStatus status) {
        this.status = status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(final String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
