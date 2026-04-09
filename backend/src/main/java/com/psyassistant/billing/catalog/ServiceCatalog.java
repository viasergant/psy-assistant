package com.psyassistant.billing.catalog;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.scheduling.domain.SessionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Master record in the service catalog.
 *
 * <p>Extends {@link BaseEntity} to track creator identity.
 * Price is NOT stored here — current/historical prices live in
 * {@link ServiceCatalogPriceHistory}.
 */
@Entity
@Table(
    name = "service_catalog",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_catalog_name_category",
        columnNames = {"name", "category"}
    )
)
public class ServiceCatalog extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    protected ServiceCatalog() {
        // JPA
    }

    public ServiceCatalog(final String name, final String category,
                          final SessionType sessionType, final int durationMin) {
        this.name = name;
        this.category = category;
        this.sessionType = sessionType;
        this.durationMin = durationMin;
        this.status = ServiceStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(final SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(final int durationMin) {
        this.durationMin = durationMin;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(final ServiceStatus status) {
        this.status = status;
    }
}
