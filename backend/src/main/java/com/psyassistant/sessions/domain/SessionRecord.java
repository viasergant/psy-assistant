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
import java.time.Instant;
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
 *     <li><strong>Discriminated union</strong>: {@link RecordKind} ({@code INDIVIDUAL} or {@code GROUP})
 *         is set at creation time and is immutable thereafter.</li>
 *     <li><strong>Immutable context</strong>: Client, therapist, date, time, type, and duration
 *         fields are copied from the appointment and never changed</li>
 *     <li><strong>One-to-one mapping</strong>: Each appointment can have at most one session record
 *         (enforced by unique constraint on appointment_id)</li>
 *     <li><strong>Lifecycle tracking</strong>: Status field tracks session progression
 *         (PENDING → IN_PROGRESS → COMPLETED)</li>
 *     <li><strong>Audit trail</strong>: Inherits automatic audit fields from {@link BaseEntity}</li>
 * </ul>
 *
 * <p>For GROUP records, {@link #clientId} is null; participants are linked via
 * the {@code session_participant} table and managed by {@code GroupSessionRecordService}.
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

    /**
     * Discriminator that identifies whether this is an INDIVIDUAL or GROUP session.
     * Set at creation; never updated (enforced by {@code updatable = false}).
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "record_kind", nullable = false, updatable = false, columnDefinition = "record_kind")
    private RecordKind recordKind = RecordKind.INDIVIDUAL;

    /**
     * Foreign key to client. Immutable snapshot from appointment.
     * Null for GROUP session records — participants are in {@code session_participant}.
     * Non-null for INDIVIDUAL records (enforced at application layer).
     */
    @Column(name = "client_id", nullable = true, updatable = false)
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

    /** Clinical notes entered by therapist when completing the session (required for COMPLETED status). */
    @Column(name = "session_notes", columnDefinition = "TEXT")
    private String sessionNotes;

    /** Actual end time if different from scheduled (optional, populated when session is completed). */
    @Column(name = "actual_end_time")
    private LocalTime actualEndTime;

    // ========== Attendance Outcome ==========

    /** Attendance outcome recorded after the session. Null until outcome is explicitly recorded. */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_outcome", columnDefinition = "attendance_outcome_type")
    private AttendanceOutcome attendanceOutcome;

    /**
     * Timestamp when the cancellation occurred.
     * Used to evaluate whether the cancellation qualifies as a late cancellation.
     */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * UUID of the user who initiated the cancellation.
     * Used to distinguish therapist cancellations from client-initiated ones.
     */
    @Column(name = "cancellation_initiator_id")
    private UUID cancellationInitiatorId;

    /**
     * Default constructor for JPA.
     */
    protected SessionRecord() {
    }

    /**
     * Creates a new INDIVIDUAL session record from an appointment.
     *
     * @param appointmentId appointment UUID
     * @param clientId client UUID (must be non-null for INDIVIDUAL records)
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
        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null for INDIVIDUAL session records");
        }
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.therapistId = therapistId;
        this.sessionDate = sessionDate;
        this.scheduledStartTime = scheduledStartTime;
        this.sessionType = sessionType;
        this.plannedDuration = plannedDuration;
        this.status = status;
        this.recordKind = RecordKind.INDIVIDUAL;
    }

    /**
     * Creates a new GROUP session record from an appointment.
     *
     * <p>Client participants are not set here; they must be linked separately
     * via {@code GroupSessionRecordService} using the {@code session_participant} table.
     *
     * @param appointmentId appointment UUID
     * @param therapistId therapist UUID
     * @param sessionDate session date
     * @param scheduledStartTime scheduled start time
     * @param sessionType session type entity
     * @param plannedDuration planned duration
     * @param status initial status
     */
    public static SessionRecord forGroup(final UUID appointmentId,
                                          final UUID therapistId,
                                          final LocalDate sessionDate,
                                          final LocalTime scheduledStartTime,
                                          final SessionType sessionType,
                                          final Duration plannedDuration,
                                          final SessionStatus status) {
        final SessionRecord record = new SessionRecord();
        record.appointmentId = appointmentId;
        record.clientId = null;
        record.therapistId = therapistId;
        record.sessionDate = sessionDate;
        record.scheduledStartTime = scheduledStartTime;
        record.sessionType = sessionType;
        record.plannedDuration = plannedDuration;
        record.status = status;
        record.recordKind = RecordKind.GROUP;
        return record;
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
     * Completes the session with required clinical notes and optional actual end time.
     *
     * @param sessionNotes clinical notes (required)
     * @param actualEndTime actual end time (optional, may be null)
     */
    public void complete(final String sessionNotes, final LocalTime actualEndTime) {
        if (sessionNotes == null || sessionNotes.trim().isEmpty()) {
            throw new IllegalArgumentException("Session notes are required to complete a session");
        }
        this.status = SessionStatus.COMPLETED;
        this.sessionNotes = sessionNotes.trim();
        this.actualEndTime = actualEndTime;
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

    public RecordKind getRecordKind() {
        return recordKind;
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

    public String getSessionNotes() {
        return sessionNotes;
    }

    public void setSessionNotes(final String sessionNotes) {
        this.sessionNotes = sessionNotes;
    }

    public LocalTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(final LocalTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public AttendanceOutcome getAttendanceOutcome() {
        return attendanceOutcome;
    }

    public void setAttendanceOutcome(final AttendanceOutcome attendanceOutcome) {
        this.attendanceOutcome = attendanceOutcome;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(final Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public UUID getCancellationInitiatorId() {
        return cancellationInitiatorId;
    }

    public void setCancellationInitiatorId(final UUID cancellationInitiatorId) {
        this.cancellationInitiatorId = cancellationInitiatorId;
    }
}
