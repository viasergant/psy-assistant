package com.psyassistant.crm.leads;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a CRM intake lead.
 *
 * <p>Extends {@link BaseEntity} to inherit the UUID primary key and Spring Data Auditing
 * fields ({@code createdAt}, {@code updatedAt}, {@code createdBy}).
 *
 * <p>A lead must have at least one {@link LeadContactMethod} (EMAIL or PHONE). This
 * invariant is enforced by the application layer (service validation) and also by a
 * database trigger ({@code trg_lead_contact_min}).
 */
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    /** Full display name for the lead. */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** How the lead was acquired (e.g. "referral", "website"). */
    @Column(length = 100)
    private String source;

    /** Current lead lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeadStatus status = LeadStatus.NEW;

    /** Optional UUID of the staff member responsible for this lead. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Free-text notes about the lead. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp of the most recent outbound or inbound contact with this lead.
     * Updated only by the system (e.g., on status transition to CONTACTED).
     */
    @Column(name = "last_contact_date")
    private Instant lastContactDate;

    /** All contact methods (email and/or phone) registered for this lead. */
    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeadContactMethod> contactMethods = new ArrayList<>();

    /** Required by JPA. */
    protected Lead() {
    }

    /**
     * Creates a new lead with a required full name.
     *
     * @param fullName the lead's full display name
     */
    public Lead(final String fullName) {
        this.fullName = fullName;
    }

    /** Returns the lead's full display name. */
    public String getFullName() {
        return fullName;
    }

    /** Sets the lead's full display name. */
    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    /** Returns the acquisition source (may be null). */
    public String getSource() {
        return source;
    }

    /** Sets the acquisition source. */
    public void setSource(final String source) {
        this.source = source;
    }

    /** Returns the current lifecycle status. */
    public LeadStatus getStatus() {
        return status;
    }

    /** Sets the lifecycle status directly (use only from service code). */
    public void setStatus(final LeadStatus status) {
        this.status = status;
    }

    /** Returns the owning staff member's UUID (may be null). */
    public UUID getOwnerId() {
        return ownerId;
    }

    /** Sets the owning staff member's UUID. */
    public void setOwnerId(final UUID ownerId) {
        this.ownerId = ownerId;
    }

    /** Returns the free-text notes (may be null). */
    public String getNotes() {
        return notes;
    }

    /** Sets the free-text notes. */
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    /** Returns the timestamp of last contact with the lead (may be null). */
    public Instant getLastContactDate() {
        return lastContactDate;
    }

    /** Sets the last contact timestamp (system-controlled only). */
    public void setLastContactDate(final Instant lastContactDate) {
        this.lastContactDate = lastContactDate;
    }

    /**
     * Returns an unmodifiable view of the contact methods list.
     * Mutate via {@link #replaceContactMethods(List)}.
     */
    public List<LeadContactMethod> getContactMethods() {
        return Collections.unmodifiableList(contactMethods);
    }

    /**
     * Replaces all existing contact methods with the provided list.
     *
     * <p>Uses in-place clear + addAll so that JPA orphan removal fires correctly.
     *
     * @param methods the new contact methods to associate
     */
    public void replaceContactMethods(final List<LeadContactMethod> methods) {
        contactMethods.clear();
        contactMethods.addAll(methods);
    }
}
