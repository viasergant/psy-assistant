package com.psyassistant.billing.pkg;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single sold (purchased) instance of a {@link PrepaidPackageDefinition}.
 *
 * <p>Tracks remaining sessions and expiry. {@link PackageBalanceAudit} records
 * every deduction for full auditability.
 */
@Entity
@Table(name = "prepaid_package_instance")
public class PrepaidPackageInstance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false, updatable = false)
    private PrepaidPackageDefinition definition;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "sessions_remaining", nullable = false)
    private int sessionsRemaining;

    @Column(name = "sessions_total", nullable = false, updatable = false)
    private int sessionsTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PackageInstanceStatus status = PackageInstanceStatus.ACTIVE;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "updated_by")
    private String updatedBy;

    protected PrepaidPackageInstance() {
    }

    public PrepaidPackageInstance(final PrepaidPackageDefinition definition,
                                  final UUID clientId,
                                  final Instant purchasedAt,
                                  final int sessionsTotal,
                                  final LocalDate expiresAt) {
        this.definition = definition;
        this.clientId = clientId;
        this.purchasedAt = purchasedAt;
        this.sessionsTotal = sessionsTotal;
        this.sessionsRemaining = sessionsTotal;
        this.expiresAt = expiresAt;
    }

    public PrepaidPackageDefinition getDefinition() {
        return definition;
    }

    public UUID getClientId() {
        return clientId;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(final UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getSessionsRemaining() {
        return sessionsRemaining;
    }

    public void setSessionsRemaining(final int sessionsRemaining) {
        this.sessionsRemaining = sessionsRemaining;
    }

    public int getSessionsTotal() {
        return sessionsTotal;
    }

    public PackageInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(final PackageInstanceStatus status) {
        this.status = status;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
