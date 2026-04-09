package com.psyassistant.billing.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Append-only record in the default price history for a service.
 *
 * <p>This entity is intentionally NOT extended from {@link com.psyassistant.common.audit.SimpleBaseEntity}
 * because {@code @LastModifiedDate} would corrupt the immutable contract.
 * All columns are {@code updatable = false} except {@code effective_to},
 * which is set exactly once during price close-out.
 * {@code @DynamicUpdate} ensures only {@code effective_to} is included in UPDATE statements.
 */
@Entity
@Table(name = "service_catalog_price_history")
@EntityListeners(AuditingEntityListener.class)
@DynamicUpdate
public class ServiceCatalogPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private ServiceCatalog serviceCatalog;

    @Column(name = "price", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal price;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;

    /** Null means this is the current open record. Set exactly once when superseded. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "changed_by", nullable = false, updatable = false, length = 255)
    private String changedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ServiceCatalogPriceHistory() {
        // JPA
    }

    public ServiceCatalogPriceHistory(final ServiceCatalog serviceCatalog,
                                      final BigDecimal price,
                                      final LocalDate effectiveFrom,
                                      final String changedBy) {
        this.serviceCatalog = serviceCatalog;
        this.price = price;
        this.effectiveFrom = effectiveFrom;
        this.changedBy = changedBy;
    }

    public UUID getId() {
        return id;
    }

    public ServiceCatalog getServiceCatalog() {
        return serviceCatalog;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(final LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
