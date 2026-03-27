package com.psyassistant.crm.clients;

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
 * A single contact channel (EMAIL or PHONE) belonging to a {@link Client}.
 *
 * <p>Mirrors the structure of {@code LeadContactMethod} for consistency.
 */
@Entity
@Table(name = "client_contact_methods")
public class ClientContactMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Contact channel: EMAIL or PHONE. */
    @Column(nullable = false, length = 20)
    private String type;

    /** The actual email address or phone number. */
    @Column(nullable = false, length = 255)
    private String value;

    /** Whether this is the preferred contact method for the client. */
    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    /** Required by JPA. */
    protected ClientContactMethod() {
    }

    /**
     * Creates a contact method for the given client.
     *
     * @param client  owning client
     * @param type    contact type (EMAIL or PHONE)
     * @param value   the actual email address or phone number
     * @param primary whether this is the primary contact method
     */
    public ClientContactMethod(final Client client, final String type,
                                final String value, final boolean primary) {
        this.client = client;
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

    /** Returns the owning client. */
    public Client getClient() {
        return client;
    }

    /** Sets the owning client. */
    public void setClient(final Client client) {
        this.client = client;
    }
}
