package com.psyassistant.crm.clients.audit;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Immutable audit header for a client profile mutation.
 */
@Entity
@Table(name = "client_profile_audit_entry")
public class ClientProfileAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "section", length = 64)
    private String section;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientProfileAuditChange> changes = new ArrayList<>();

    /** Required by JPA. */
    protected ClientProfileAuditEntry() {
    }

    /**
     * Creates an immutable audit entry header.
     */
    public ClientProfileAuditEntry(final UUID clientId, final UUID actorUserId,
                                   final String actorName, final String eventType,
                                   final String section, final String requestId) {
        this.clientId = clientId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.eventType = eventType;
        this.section = section;
        this.requestId = requestId;
    }

    /** Adds a field change row under this audit entry. */
    public void addChange(final String fieldName, final String oldValue, final String newValue) {
        changes.add(new ClientProfileAuditChange(this, fieldName, oldValue, newValue));
    }
}
