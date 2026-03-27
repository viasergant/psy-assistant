package com.psyassistant.crm.leads;

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
 * A single contact channel (EMAIL or PHONE) belonging to a {@link Lead}.
 *
 * <p>At least one contact method is required per lead; this invariant is enforced by
 * both a database trigger ({@code trg_lead_contact_min}) and application-layer validation.
 */
@Entity
@Table(name = "lead_contact_methods")
public class LeadContactMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    /** Contact channel: EMAIL or PHONE. */
    @Column(nullable = false, length = 20)
    private String type;

    /** The actual email address or phone number. */
    @Column(nullable = false, length = 255)
    private String value;

    /** Whether this is the preferred contact method for the lead. */
    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    /** Required by JPA. */
    protected LeadContactMethod() {
    }

    /**
     * Creates a contact method for the given lead.
     *
     * @param lead      owning lead
     * @param type      contact type (EMAIL or PHONE)
     * @param value     the actual email address or phone number
     * @param primary   whether this is the primary contact method
     */
    public LeadContactMethod(final Lead lead, final String type,
                              final String value, final boolean primary) {
        this.lead = lead;
        this.type = type;
        this.value = value;
        this.primary = primary;
    }

    /** Returns the record's primary key. */
    public UUID getId() {
        return id;
    }

    /** Returns the contact type (EMAIL or PHONE). */
    public String getType() {
        return type;
    }

    /** Sets the contact type. */
    public void setType(final String type) {
        this.type = type;
    }

    /** Returns the contact value (email address or phone number). */
    public String getValue() {
        return value;
    }

    /** Sets the contact value. */
    public void setValue(final String value) {
        this.value = value;
    }

    /** Returns true if this is the primary contact method. */
    public boolean isPrimary() {
        return primary;
    }

    /** Sets whether this is the primary contact method. */
    public void setPrimary(final boolean primary) {
        this.primary = primary;
    }

    /** Returns the owning lead. */
    public Lead getLead() {
        return lead;
    }

    /** Sets the owning lead. */
    public void setLead(final Lead lead) {
        this.lead = lead;
    }
}
