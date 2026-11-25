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
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Master record in the service catalog.
 *
 * <p>Extends {@link BaseEntity} to track creator identity.
 * Price is NOT stored here — current/historical prices live in
 * {@link ServiceCatalogPriceHistory}.
 */
@Filter(name = "orgFilter", condition = "org_id = :orgId")
@Entity
@Table(
    name = "service_catalog",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_catalog_org_name",
        columnNames = {"org_id", "name"}
    )
)
public class ServiceCatalog extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    protected ServiceCatalog() {
        // JPA
    }

    public ServiceCatalog(final String name,
                          final SessionType sessionType, final int durationMin) {
        this.name = name;
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

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(final UUID orgId) {
        this.orgId = orgId;
    }
}
