package com.psyassistant.billing.payment;

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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable ledger entry representing a single payment applied to an invoice.
 *
 * <p>Once persisted, payment records must never be modified. There is no {@code updated_at}
 * column. Corrections are handled by creating a {@link Refund}.
 *
 * <p>Does NOT extend {@link com.psyassistant.common.audit.BaseEntity} because that
 * adds {@code updated_at} / {@code updated_by} audit fields which are inappropriate
 * for an immutable record.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20, updatable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDate paymentDate;

    @Column(name = "reference", length = 255, updatable = false)
    private String reference;

    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String notes;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() { }

    /**
     * Creates a new immutable payment record.
     *
     * @param invoiceId     UUID of the invoice being paid
     * @param amount        positive payment amount
     * @param paymentMethod how the payment was made
     * @param paymentDate   actual date of payment
     * @param reference     optional bank reference or transaction number
     * @param notes         optional free-text notes
     * @param createdBy     name of the user recording the payment
     */
    public Payment(
            final UUID invoiceId,
            final BigDecimal amount,
            final PaymentMethod paymentMethod,
            final LocalDate paymentDate,
            final String reference,
            final String notes,
            final String createdBy) {
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.reference = reference;
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

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getReference() {
        return reference;
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
