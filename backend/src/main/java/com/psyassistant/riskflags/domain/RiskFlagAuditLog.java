package com.psyassistant.riskflags.domain;

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
 * Append-only audit record for risk flag mutations.
 *
 * <p>There is intentionally NO foreign-key constraint on {@code flag_id} so that
 * audit entries survive even if a flag is ever hard-deleted in the future.
 * No update or delete methods are provided at any layer.
 */
@Entity
@Table(name = "risk_flag_audit_log")
public class RiskFlagAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Reference to the risk flag — intentionally no FK constraint. */
    @Column(name = "flag_id", nullable = false, updatable = false)
    private UUID flagId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "actor_name", nullable = false, updatable = false, length = 255)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, updatable = false, length = 30)
    private RiskFlagAuditActionType actionType;

    @Column(name = "flag_type_name", nullable = false, updatable = false, length = 100)
    private String flagTypeName;

    @Column(name = "status", nullable = false, updatable = false, length = 20)
    private String status;

    /** Server-managed timestamp; set by DB DEFAULT NOW(). */
    @Column(name = "action_timestamp", insertable = false, updatable = false)
    private Instant actionTimestamp;

    // ---- constructors ----

    protected RiskFlagAuditLog() { }

    /**
     * Creates a new immutable audit log entry.
     *
     * @param flagId       UUID of the flag being audited (no FK — intentional)
     * @param clientId     UUID of the client the flag belongs to
     * @param actorUserId  UUID of the user performing the action
     * @param actorName    display name of the actor at the time of the action
     * @param actionType   type of action performed
     * @param flagTypeName name of the flag type at the time of the action
     * @param status       flag status after the action
     */
    public RiskFlagAuditLog(final UUID flagId, final UUID clientId,
                             final UUID actorUserId, final String actorName,
                             final RiskFlagAuditActionType actionType,
                             final String flagTypeName, final String status) {
        this.flagId = flagId;
        this.clientId = clientId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actionType = actionType;
        this.flagTypeName = flagTypeName;
        this.status = status;
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public UUID getFlagId() {
        return flagId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public RiskFlagAuditActionType getActionType() {
        return actionType;
    }

    public String getFlagTypeName() {
        return flagTypeName;
    }

    public String getStatus() {
        return status;
    }

    public Instant getActionTimestamp() {
        return actionTimestamp;
    }
}
