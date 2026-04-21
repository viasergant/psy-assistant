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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Links a GROUP {@link SessionRecord} to a client participant.
 *
 * <p>Each active row represents one client in the group session.
 * {@code removedAt} is set when the client is removed for audit purposes
 * (the row itself is kept in the database; the timeline entry is hard-deleted
 * separately by the caller).
 *
 * <p>A DB trigger enforces the ≤ 20 active-participant cap.
 */
@Entity
@Table(name = "session_participant")
@EntityListeners(AuditingEntityListener.class)
public class SessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** FK to the parent group session record. */
    @Column(name = "session_record_id", nullable = false, updatable = false)
    private UUID sessionRecordId;

    /** Client participating in the group session. */
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    /** When the participant joined (usually the session creation timestamp). */
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    /**
     * When the participant was removed (null = still active).
     * Set when a participant is removed; the row is retained for audit traceability.
     */
    @Column(name = "removed_at")
    private Instant removedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected SessionParticipant() {
    }

    /**
     * Creates a new active session participant.
     *
     * @param sessionRecordId group session record UUID
     * @param clientId        client UUID
     */
    public SessionParticipant(final UUID sessionRecordId, final UUID clientId) {
        this.sessionRecordId = sessionRecordId;
        this.clientId = clientId;
        this.joinedAt = Instant.now();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Business methods
    // ─────────────────────────────────────────────────────────────────────

    /** Returns true if this participant is still active (not removed). */
    public boolean isActive() {
        return removedAt == null;
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

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(final Instant removedAt) {
        this.removedAt = removedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
