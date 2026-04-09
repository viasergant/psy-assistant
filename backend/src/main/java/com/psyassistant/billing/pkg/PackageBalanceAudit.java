package com.psyassistant.billing.pkg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entry for a prepaid package balance change.
 *
 * <p>One row is written for every DEDUCT, EXPIRE, or MANUAL_ADJUST action.
 * Never updated or deleted — provides a full ledger trail.
 */
@Entity
@Table(name = "package_balance_audit")
public class PackageBalanceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_instance_id", nullable = false, updatable = false)
    private PrepaidPackageInstance packageInstance;

    @Column(name = "session_id", updatable = false)
    private UUID sessionId;

    @Column(name = "therapist_id", updatable = false)
    private UUID therapistId;

    @Column(name = "balance_before", nullable = false, updatable = false)
    private int balanceBefore;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30, updatable = false)
    private BalanceAuditAction action;

    @Column(name = "actor", nullable = false, length = 255, updatable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PackageBalanceAudit() {
    }

    public PackageBalanceAudit(final PrepaidPackageInstance packageInstance,
                               final UUID sessionId,
                               final UUID therapistId,
                               final int balanceBefore,
                               final int balanceAfter,
                               final BalanceAuditAction action,
                               final String actor) {
        this.packageInstance = packageInstance;
        this.sessionId = sessionId;
        this.therapistId = therapistId;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.action = action;
        this.actor = actor;
        this.createdBy = actor;
        this.occurredAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public PrepaidPackageInstance getPackageInstance() {
        return packageInstance;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public int getBalanceBefore() {
        return balanceBefore;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public BalanceAuditAction getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
