package com.psyassistant.therapists.domain;

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
 * Immutable audit entry recording a single mutation event on a therapist profile.
 * Links to zero or more {@link TherapistProfileAuditChange} records detailing
 * field-level changes.
 */
@Entity
@Table(name = "therapist_profile_audit_entry")
public class TherapistProfileAuditEntry {

    /** Unique identifier for this audit entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID of the therapist profile that was modified. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** ID of the user who performed the action (null for system actions). */
    @Column(name = "actor_user_id")
    private UUID actorUserId;

    /** Name of the actor for display purposes (copied from principal). */
    @Column(name = "actor_name", length = 255)
    private String actorName;

    /** Type of event (e.g., "CREATE", "UPDATE", "DEACTIVATE"). */
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
        orphanRemoval = true
    )
    private List<TherapistProfileAuditChange> changes = new ArrayList<>();

    // Constructors
    public TherapistProfileAuditEntry() { }

    public TherapistProfileAuditEntry(UUID therapistProfileId, UUID actorUserId,
                                      String actorName, String eventType, String requestId) {
        this.therapistProfileId = therapistProfileId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.eventType = eventType;
        this.requestId = requestId;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<TherapistProfileAuditChange> getChanges() {
        return changes;
    }

    public void setChanges(List<TherapistProfileAuditChange> changes) {
        this.changes = changes;
    }
}
