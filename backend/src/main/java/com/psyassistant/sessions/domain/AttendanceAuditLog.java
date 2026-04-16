package com.psyassistant.sessions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit record for attendance outcome changes.
 *
 * <p>Every time an attendance outcome is set or changed on a {@link SessionRecord},
 * a new entry is appended here. The table has no FK on {@code session_record_id}
 * to preserve history even if the session record is ever deleted.
 */
@Entity
@Table(name = "attendance_audit_log")
public class AttendanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Session record that was updated. */
    @Column(name = "session_record_id", nullable = false)
    private UUID sessionRecordId;

    /** User who changed the outcome (null for system-generated changes). */
    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    /** When the change was recorded. */
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /** Previous attendance outcome (null if this is the first outcome recorded). */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_outcome", columnDefinition = "attendance_outcome_type")
    private AttendanceOutcome previousOutcome;

    /** New attendance outcome. */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_outcome", nullable = false, columnDefinition = "attendance_outcome_type")
    private AttendanceOutcome newOutcome;

    /** Optional free-text note about the change. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Required by JPA. */
    protected AttendanceAuditLog() {
    }

    /**
     * Creates a new attendance audit log entry.
     *
     * @param sessionRecordId   session record UUID
     * @param changedByUserId   user who made the change (may be null)
     * @param changedAt         timestamp of change
     * @param previousOutcome   previous outcome value (may be null)
     * @param newOutcome        new outcome value
     */
    public AttendanceAuditLog(
            final UUID sessionRecordId,
            final UUID changedByUserId,
            final Instant changedAt,
            final AttendanceOutcome previousOutcome,
            final AttendanceOutcome newOutcome) {
        this.sessionRecordId = sessionRecordId;
        this.changedByUserId = changedByUserId;
        this.changedAt = changedAt;
        this.previousOutcome = previousOutcome;
        this.newOutcome = newOutcome;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionRecordId() {
        return sessionRecordId;
    }

    public UUID getChangedByUserId() {
        return changedByUserId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public AttendanceOutcome getPreviousOutcome() {
        return previousOutcome;
    }

    public AttendanceOutcome getNewOutcome() {
        return newOutcome;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }
}
