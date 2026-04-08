package com.psyassistant.billing.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit ledger for client wallet movements.
 */
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30, updatable = false)
    private WalletTransactionType transactionType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String notes;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletTransaction() { }

    public WalletTransaction(
            final UUID walletId,
            final BigDecimal amount,
            final WalletTransactionType transactionType,
            final UUID referenceId,
            final String notes,
            final String createdBy) {
        this.walletId = walletId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    @PrePersist
    private void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public WalletTransactionType getTransactionType() {
        return transactionType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
