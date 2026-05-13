package com.psyassistant.riskflags.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Configurable risk flag type (e.g. "Self-Harm Risk").
 *
 * <p>Treated as mostly immutable configuration data. Deactivation is the only
 * permitted mutation; the type is never hard-deleted so that existing flag records
 * retain a resolvable reference.
 */
@Entity
@Table(name = "risk_flag_types")
public class RiskFlagType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_order")
    private short displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    /** Server-managed creation timestamp; not settable by the application. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // ---- constructors ----

    protected RiskFlagType() { }

    /**
     * Creates a new active flag type.
     *
     * @param name         unique label shown in the UI (max 100 chars)
     * @param displayOrder relative sort position in pick-lists
     */
    public RiskFlagType(final String name, final short displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    // ---- business methods ----

    /** Soft-deletes this flag type so it no longer appears in the creation form. */
    public void deactivate() {
        this.active = false;
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public short getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
