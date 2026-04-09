package com.psyassistant.billing.pkg;

import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Catalogue entry defining a sellable prepaid session package.
 *
 * <p>Instances are sold to clients via {@link PrepaidPackageInstance}.
 * Once sold, definitions should be ARCHIVED rather than deleted.
 */
@Entity
@Table(name = "prepaid_package_definition")
public class PrepaidPackageDefinition extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ServiceType serviceType;

    @Column(name = "session_qty", nullable = false)
    private int sessionQty;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PackageDefinitionStatus status = PackageDefinitionStatus.ACTIVE;

    @Column(name = "updated_by")
    private String updatedBy;

    protected PrepaidPackageDefinition() {
    }

    public PrepaidPackageDefinition(final String name,
                                    final ServiceType serviceType,
                                    final int sessionQty,
                                    final java.math.BigDecimal price) {
        this.name = name;
        this.serviceType = serviceType;
        this.sessionQty = sessionQty;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public int getSessionQty() {
        return sessionQty;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final java.math.BigDecimal price) {
        this.price = price;
    }

    public PackageDefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(final PackageDefinitionStatus status) {
        this.status = status;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
