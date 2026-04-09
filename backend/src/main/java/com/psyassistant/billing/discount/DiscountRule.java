package com.psyassistant.billing.discount;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A reusable discount rule that can be scoped to a specific client or service.
 *
 * <p>Constraint: when scope=CLIENT, clientId must be set and serviceCatalogId must be null,
 * and vice versa for scope=SERVICE.
 */
@Entity
@Table(name = "discount_rules")
public class DiscountRule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DiscountType type;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private DiscountScope scope;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "service_catalog_id")
    private UUID serviceCatalogId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "updated_by")
    private String updatedBy;

    protected DiscountRule() {
    }

    public DiscountRule(final String name,
                        final DiscountType type,
                        final BigDecimal value,
                        final DiscountScope scope,
                        final UUID clientId,
                        final UUID serviceCatalogId) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.scope = scope;
        this.clientId = clientId;
        this.serviceCatalogId = serviceCatalogId;
    }

    public String getName() {
        return name;
    }

    public DiscountType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public DiscountScope getScope() {
        return scope;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getServiceCatalogId() {
        return serviceCatalogId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
