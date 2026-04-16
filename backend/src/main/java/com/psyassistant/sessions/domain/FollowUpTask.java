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
 * A follow-up task created when a session has a no-show attendance outcome.
 *
 * <p>The {@code session_record_id} column has a UNIQUE constraint to ensure
 * at most one follow-up task exists per session (idempotency guard).
 */
@Entity
@Table(name = "session_follow_up_tasks")
public class FollowUpTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Session record this task relates to. UNIQUE — one task per session. */
    @Column(name = "session_record_id", nullable = false, unique = true)
    private UUID sessionRecordId;

    /** User assigned to action the follow-up. */
    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    /** Type of follow-up task. */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, columnDefinition = "follow_up_task_type")
    private FollowUpTaskType taskType;

    /** Current status of the task. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "follow_up_task_status")
    private FollowUpTaskStatus status = FollowUpTaskStatus.PENDING;

    /** When this task was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** When this task was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected FollowUpTask() {
    }

    /**
     * Creates a new follow-up task.
     *
     * @param sessionRecordId   session record UUID
     * @param assignedToUserId  user to assign the task to (may be null)
     * @param taskType          type of follow-up required
     */
    public FollowUpTask(
            final UUID sessionRecordId,
            final UUID assignedToUserId,
            final FollowUpTaskType taskType) {
        this.sessionRecordId = sessionRecordId;
        this.assignedToUserId = assignedToUserId;
        this.taskType = taskType;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionRecordId() {
        return sessionRecordId;
    }

    public UUID getAssignedToUserId() {
        return assignedToUserId;
    }

    public FollowUpTaskType getTaskType() {
        return taskType;
    }

    public FollowUpTaskStatus getStatus() {
        return status;
    }

    public void setStatus(final FollowUpTaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
