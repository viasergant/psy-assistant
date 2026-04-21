package com.psyassistant.sessions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Per-client attendance outcome for a GROUP {@link SessionRecord}.
 *
 * <p>Each row tracks the attendance status of a single client within a group session.
 * Outcomes can differ between participants in the same session.
 * Unique constraint on (session_record_id, client_id) ensures only one record per participant.
 *
 * <p>Publishing {@link com.psyassistant.sessions.event.AttendanceOutcomeRecordedEvent}
 * after saving triggers PA-41 escalation logic independently for each client.
 */
@Entity
@Table(name = "group_session_attendance")
@EntityListeners(AuditingEntityListener.class)
public class GroupSessionAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** FK to the parent group session record. */
    @Column(name = "session_record_id", nullable = false, updatable = false)
    private UUID sessionRecordId;

    /** Client whose attendance is being recorded. */
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    /** The recorded attendance outcome for this client. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "attendance_outcome", nullable = false, columnDefinition = "attendance_outcome_type")
    private AttendanceOutcome attendanceOutcome;

    /** Cancellation timestamp (non-null if outcome is LATE_CANCELLATION or CANCELLED). */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** UUID of the user who initiated the cancellation (null for non-cancellation outcomes). */
    @Column(name = "cancellation_initiator_id")
    private UUID cancellationInitiatorId;

    /** When the attendance outcome was recorded. */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /** UUID of the user who recorded the outcome. */
    @Column(name = "recorded_by")
    private UUID recordedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected GroupSessionAttendance() {
    }

    /**
     * Records a new per-client attendance outcome for a group session.
     *
     * @param sessionRecordId session record UUID
     * @param clientId        client UUID
     * @param outcome         attendance outcome
     * @param recordedBy      user UUID who recorded the outcome
     */
    public GroupSessionAttendance(
            final UUID sessionRecordId,
            final UUID clientId,
            final AttendanceOutcome outcome,
            final UUID recordedBy) {
        this.sessionRecordId = sessionRecordId;
        this.clientId = clientId;
        this.attendanceOutcome = outcome;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Getters / Setters
    // ─────────────────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public UUID getSessionRecordId() {
        return sessionRecordId;
    }

    public UUID getClientId() {
        return clientId;
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
