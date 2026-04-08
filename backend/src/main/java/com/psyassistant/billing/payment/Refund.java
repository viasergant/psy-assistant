package com.psyassistant.billing.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable ledger entry representing a refund issued against a paid invoice.
 *
 * <p>Refunds are append-only. Once created they cannot be updated.
 * The invoice remains in PAID status after a refund is registered.
 */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Column(name = "payment_id", updatable = false)
    private UUID paymentId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "refund_date", nullable = false, updatable = false)
    private LocalDate refundDate;

    @Column(name = "reference", length = 255, updatable = false)
    private String reference;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Refund() { }

    /**
     * Creates a new immutable refund record.
     *
     * @param invoiceId   UUID of the invoice being refunded
     * @param paymentId   optional UUID of the specific payment to refund against
     * @param amount      positive refund amount
     * @param reason      mandatory reason for the refund
     * @param refundDate  date the refund was issued
     * @param reference   optional reference number
     * @param createdBy   name of the user recording the refund
     */
    public Refund(
            final UUID invoiceId,
            final UUID paymentId,
            final BigDecimal amount,
            final String reason,
            final LocalDate refundDate,
            final String reference,
            final String createdBy) {
        this.invoiceId = invoiceId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.refundDate = refundDate;
        this.reference = reference;
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

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public LocalDate getRefundDate() {
        return refundDate;
    }

    public String getReference() {
        return reference;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
