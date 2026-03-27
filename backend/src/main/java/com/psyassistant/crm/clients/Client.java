package com.psyassistant.crm.clients;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a fully converted client record created from a CRM lead.
 *
 * <p>Extends {@link BaseEntity} to inherit the UUID primary key and Spring Data Auditing
 * fields ({@code createdAt}, {@code updatedAt}, {@code createdBy}).
 *
 * <p>The {@code sourceLeadId} establishes a back-link to the originating lead and carries
 * a UNIQUE database constraint — this is the authoritative concurrency guard that prevents
 * a lead from being converted twice.
 */
@Entity
@Table(name = "clients")
public class Client extends BaseEntity {

    /** Full display name for the client. */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** UUID of the staff member responsible for this client. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Free-text notes about the client (may include pre-conversion lead notes). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * UUID of the lead this client was converted from.
     * Carries a UNIQUE constraint enforced at the database level.
     */
    @Column(name = "source_lead_id", unique = true)
    private UUID sourceLeadId;

    /** All contact methods (email and/or phone) registered for this client. */
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientContactMethod> contactMethods = new ArrayList<>();

    /** Required by JPA. */
    protected Client() {
    }

    /**
     * Creates a new client with a required full name.
     *
     * @param fullName the client's full display name
     */
    public Client(final String fullName) {
        this.fullName = fullName;
    }

    /** Returns the client's full display name. */
    public String getFullName() {
        return fullName;
    }

    /** Sets the client's full display name. */
    public void setFullName(final String fullName) {
        this.fullName = fullName;
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

    /** Returns the UUID of the originating lead (may be null). */
    public UUID getSourceLeadId() {
        return sourceLeadId;
    }

    /** Sets the originating lead UUID. */
    public void setSourceLeadId(final UUID sourceLeadId) {
        this.sourceLeadId = sourceLeadId;
    }

    /**
     * Returns an unmodifiable view of the contact methods list.
     * Mutate via {@link #setContactMethods(List)}.
     */
    public List<ClientContactMethod> getContactMethods() {
        return Collections.unmodifiableList(contactMethods);
    }

    /**
     * Replaces all existing contact methods with the provided list.
     *
     * <p>Uses in-place clear + addAll so that JPA orphan removal fires correctly.
     *
     * @param methods the new contact methods to associate
     */
    public void setContactMethods(final List<ClientContactMethod> methods) {
        contactMethods.clear();
        contactMethods.addAll(methods);
    }
}
