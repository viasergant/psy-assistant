package com.psyassistant.therapists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Immutable record of a single field change within an audit entry.
 * Stores the field name, its previous value, and its new value.
 */
@Entity
@Table(name = "therapist_profile_audit_change")
public class TherapistProfileAuditChange {

    /** Unique identifier for this change record. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The audit entry this change is part of. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_entry_id", nullable = false)
    private TherapistProfileAuditEntry auditEntry;

    /** Name of the field that changed. */
    @Column(name = "field_name", nullable = false, length = 128)
    private String fieldName;

    /** Previous value before the change (null if new). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** New value after the change (null if deleted). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // Constructors
    public TherapistProfileAuditChange() {}

    public TherapistProfileAuditChange(TherapistProfileAuditEntry auditEntry,
                                       String fieldName, String oldValue, String newValue) {
        this.auditEntry = auditEntry;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TherapistProfileAuditEntry getAuditEntry() {
        return auditEntry;
    }

    public void setAuditEntry(TherapistProfileAuditEntry auditEntry) {
        this.auditEntry = auditEntry;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
