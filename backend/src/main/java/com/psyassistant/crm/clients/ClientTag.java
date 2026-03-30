package com.psyassistant.crm.clients;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tag assigned to a client profile.
 */
@Entity
@Table(name = "client_tags")
public class ClientTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "tag", nullable = false, length = 64)
    private String tag;

    protected ClientTag() {
    }

    /**
     * Creates a new client tag.
     *
     * @param client owning client
     * @param tag tag value
     */
    public ClientTag(final Client client, final String tag) {
        this.client = client;
        this.tag = tag;
    }

    public Client getClient() {
        return client;
    }

    public String getTag() {
        return tag;
    }
}
