package com.psyassistant.billing.catalog;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-therapist price override for a service.
 * Only the current override is stored — no history is tracked.
 */
@Entity
@Table(
    name = "service_catalog_therapist_override",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_override_service_therapist",
        columnNames = {"service_id", "therapist_id"}
    )
)
public class ServiceCatalogTherapistOverride extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private ServiceCatalog serviceCatalog;

    @Column(name = "therapist_id", nullable = false, updatable = false)
    private UUID therapistId;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected ServiceCatalogTherapistOverride() {
        // JPA
    }

    public ServiceCatalogTherapistOverride(final ServiceCatalog serviceCatalog,
                                           final UUID therapistId,
                                           final BigDecimal price) {
        this.serviceCatalog = serviceCatalog;
        this.therapistId = therapistId;
        this.price = price;
    }

    public ServiceCatalog getServiceCatalog() {
        return serviceCatalog;
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }
}
