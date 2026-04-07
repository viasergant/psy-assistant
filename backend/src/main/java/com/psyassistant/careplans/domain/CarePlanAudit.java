package com.psyassistant.careplans.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit record for care plan mutations.
 *
 * <p>There is intentionally NO foreign-key constraint on {@code care_plan_id} so that
 * audit entries survive even after a plan is hard-deleted (though plans are only
 * soft-deleted via status transitions in practice).
 */
@Entity
@Table(name = "care_plan_audit")
public class CarePlanAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Reference to the care plan — intentionally no FK constraint. */
    @Column(name = "care_plan_id", nullable = false, updatable = false)
    private UUID carePlanId;

    @Column(name = "entity_type", length = 30, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50, updatable = false)
    private AuditActionType actionType;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "actor_name", nullable = false, length = 255, updatable = false)
    private String actorName;

    @Column(name = "action_timestamp", nullable = false, updatable = false)
    private Instant actionTimestamp;

    @Column(name = "field_name", length = 100, updatable = false)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "request_id", length = 100, updatable = false)
    private String requestId;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", updatable = false)
    private String metadata;

    // ---- constructors ----

    protected CarePlanAudit() { }

    public CarePlanAudit(final UUID carePlanId, final String entityType, final UUID entityId,
                          final AuditActionType actionType, final UUID actorUserId,
                          final String actorName, final String fieldName,
                          final String oldValue, final String newValue,
                          final String requestId) {
        this.carePlanId = carePlanId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actionType = actionType;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.requestId = requestId;
        this.actionTimestamp = Instant.now();
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public UUID getCarePlanId() {
        return carePlanId;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public Instant getActionTimestamp() {
        return actionTimestamp;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMetadata() {
        return metadata;
    }
}
