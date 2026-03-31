package com.psyassistant.scheduling.domain;

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
 * Immutable record of a single field change within a schedule audit entry.
 *
 * <p>Stores the field name, its previous value, and its new value as text.
 * Linked to the parent {@link TherapistScheduleAuditEntry}.
 */
@Entity
@Table(name = "therapist_schedule_audit_change")
public class TherapistScheduleAuditChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Parent audit entry that groups this change with others. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private TherapistScheduleAuditEntry auditEntry;

    /** Name of the field that changed (e.g., "startTime", "status"). */
    @Column(name = "field_name", nullable = false, length = 128)
    private String fieldName;

    /** Previous value as text (null for newly created fields). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** New value as text (null for deleted fields). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Default constructor for JPA.
     */
    protected TherapistScheduleAuditChange() {
    }

    /**
     * Creates a new field change record.
     *
     * @param auditEntry parent audit entry
     * @param fieldName name of the changed field
     * @param oldValue previous value (null for CREATE)
     * @param newValue new value (null for DELETE)
     */
    public TherapistScheduleAuditChange(final TherapistScheduleAuditEntry auditEntry,
                                         final String fieldName,
                                         final String oldValue,
                                         final String newValue) {
        this.auditEntry = auditEntry;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public TherapistScheduleAuditEntry getAuditEntry() {
        return auditEntry;
    }

    public void setAuditEntry(final TherapistScheduleAuditEntry auditEntry) {
        this.auditEntry = auditEntry;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(final String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(final String newValue) {
        this.newValue = newValue;
    }
}
