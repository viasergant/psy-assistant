package com.psyassistant.scheduling.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable audit entry recording a single mutation event on a therapist schedule entity.
 *
 * <p>Links to zero or more {@link TherapistScheduleAuditChange} records detailing
 * field-level changes (before/after values).
 *
 * <p>Supports auditing of:
 * <ul>
 *     <li>Recurring schedule entries (create, update, delete)</li>
 *     <li>Schedule overrides (create, update, delete)</li>
 *     <li>Leave periods (create, update, delete, approve, reject)</li>
 * </ul>
 */
@Entity
@Table(name = "therapist_schedule_audit_entry")
public class TherapistScheduleAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Foreign key to the therapist profile affected by this change. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** Type of entity modified: RECURRING_SCHEDULE, OVERRIDE, or LEAVE. */
    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    /** UUID of the specific entity that was modified. */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    /** User ID of the actor who performed the action. */
    @Column(name = "actor_user_id")
    private UUID actorUserId;

    /** Name of the actor who performed the action. */
    @Column(name = "actor_name", length = 255)
    private String actorName;

    /** Action performed: CREATE, UPDATE, DELETE, APPROVE, REJECT. */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** Optional request ID for tracing distributed calls. */
    @Column(name = "request_id", length = 64)
    private String requestId;

    /** Timestamp when this action was recorded. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Individual field-level changes associated with this entry. */
    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "auditEntry",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<TherapistScheduleAuditChange> changes = new ArrayList<>();

    /**
     * Default constructor for JPA.
     */
    protected TherapistScheduleAuditEntry() {
    }

    /**
     * Creates a new audit entry.
     *
     * @param therapistProfileId therapist profile UUID
     * @param entityType entity type (RECURRING_SCHEDULE, OVERRIDE, LEAVE)
     * @param entityId UUID of the modified entity
     * @param actorUserId actor user UUID
     * @param actorName actor display name
     * @param eventType event type (CREATE, UPDATE, DELETE, APPROVE, REJECT)
     * @param requestId optional request correlation ID
     */
    public TherapistScheduleAuditEntry(final UUID therapistProfileId,
                                        final String entityType,
                                        final UUID entityId,
                                        final UUID actorUserId,
                                        final String actorName,
                                        final String eventType,
                                        final String requestId) {
        this.therapistProfileId = therapistProfileId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.eventType = eventType;
        this.requestId = requestId;
        this.createdAt = Instant.now();
    }

    /**
     * Adds a field-level change to this audit entry.
     *
     * @param fieldName name of the changed field
     * @param oldValue previous value (null for CREATE)
     * @param newValue new value (null for DELETE)
     */
    public void addChange(final String fieldName, final String oldValue, final String newValue) {
        final TherapistScheduleAuditChange change =
            new TherapistScheduleAuditChange(this, fieldName, oldValue, newValue);
        this.changes.add(change);
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(final UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(final UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(final UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(final String actorName) {
        this.actorName = actorName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<TherapistScheduleAuditChange> getChanges() {
        return changes;
    }

    public void setChanges(final List<TherapistScheduleAuditChange> changes) {
        this.changes = changes;
    }
}
