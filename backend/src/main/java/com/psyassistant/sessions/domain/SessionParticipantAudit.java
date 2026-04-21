package com.psyassistant.sessions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit log for participant add/remove operations on GROUP session records.
 *
 * <p>Every time a client is added to or removed from a group session,
 * a new entry is appended here with actor identity and timestamp.
 * Intentionally has no FK to {@code session_record} so the audit trail
 * is preserved even if the session record is deleted.
 */
@Entity
@Table(name = "session_participant_audit")
public class SessionParticipantAudit {

    /** Append-only action discriminator. */
    public enum Action {
        ADDED,
        REMOVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Session record that was modified. */
    @Column(name = "session_record_id", nullable = false)
    private UUID sessionRecordId;

    /** Client who was added to or removed from the session. */
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /** Whether the client was added or removed. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action", nullable = false, columnDefinition = "participant_audit_action")
    private Action action;

    /** UUID of the user who performed the action (null for system-generated operations). */
    @Column(name = "actor_user_id")
    private UUID actorUserId;

    /** Display name of the actor at the time of the action. */
    @Column(name = "actor_name")
    private String actorName;

    /** When the action occurred. */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Required by JPA. */
    protected SessionParticipantAudit() {
    }

    /**
     * Creates a new audit entry.
     *
     * @param sessionRecordId session record UUID
     * @param clientId        client UUID
     * @param action          ADDED or REMOVED
     * @param actorUserId     UUID of the user performing the action (may be null)
     * @param actorName       display name of the actor (may be null)
     */
    public SessionParticipantAudit(
            final UUID sessionRecordId,
            final UUID clientId,
            final Action action,
            final UUID actorUserId,
            final String actorName) {
        this.sessionRecordId = sessionRecordId;
        this.clientId = clientId;
        this.action = action;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.occurredAt = Instant.now();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Getters (no setters — this is an append-only log)
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

    public Action getAction() {
        return action;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
