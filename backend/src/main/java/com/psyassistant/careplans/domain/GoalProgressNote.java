package com.psyassistant.careplans.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Append-only progress note for a single {@link CarePlanGoal}.
 *
 * <p>Notes are immutable once created — no PUT or DELETE endpoints exist.
 * Visibility: the authoring therapist can see their own notes; supervisors/admins
 * can see all notes (enforced in the service layer).
 */
@Entity
@Table(name = "goal_progress_note")
@EntityListeners(AuditingEntityListener.class)
public class GoalProgressNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "goal_id", nullable = false, updatable = false)
    private UUID goalId;

    @Column(name = "care_plan_id", nullable = false, updatable = false)
    private UUID carePlanId;

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String noteText;

    @Column(name = "author_user_id", nullable = false, updatable = false)
    private UUID authorUserId;

    @Column(name = "author_name", length = 255, updatable = false)
    private String authorName;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    // ---- constructors ----

    protected GoalProgressNote() { }

    public GoalProgressNote(final UUID goalId, final UUID carePlanId,
                             final String noteText,
                             final UUID authorUserId, final String authorName) {
        this.goalId = goalId;
        this.carePlanId = carePlanId;
        this.noteText = noteText;
        this.authorUserId = authorUserId;
        this.authorName = authorName;
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getCarePlanId() {
        return carePlanId;
    }

    public String getNoteText() {
        return noteText;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
