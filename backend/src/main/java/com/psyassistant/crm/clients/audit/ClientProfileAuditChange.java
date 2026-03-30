package com.psyassistant.crm.clients.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Field-level before/after change row for a client profile mutation.
 */
@Entity
@Table(name = "client_profile_audit_change")
public class ClientProfileAuditChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private ClientProfileAuditEntry entry;

    @Column(name = "field_name", nullable = false, length = 128)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Required by JPA. */
    protected ClientProfileAuditChange() {
    }

    /**
     * Creates a new field change row.
     */
    public ClientProfileAuditChange(final ClientProfileAuditEntry entry,
                                    final String fieldName,
                                    final String oldValue,
                                    final String newValue) {
        this.entry = entry;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
