package com.psyassistant.billing.discount;

import com.psyassistant.billing.invoice.InvoiceLineItem;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of a discount applied to a single invoice line item.
 *
 * <p>Never updated or deleted (use ON DELETE CASCADE if the line item is removed).
 */
@Entity
@Table(name = "invoice_line_item_discounts")
public class InvoiceLineItemDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_item_id", nullable = false, updatable = false)
    private InvoiceLineItem lineItem;

    /**
     * Null when this is a manual override (no source rule).
     */
    @Column(name = "discount_rule_id", updatable = false)
    private UUID discountRuleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20, updatable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal discountValue;

    @Column(name = "cap_applied", nullable = false, updatable = false)
    private boolean capApplied;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal discountAmount;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected InvoiceLineItemDiscount() {
    }

    public InvoiceLineItemDiscount(final InvoiceLineItem lineItem,
                                   final UUID discountRuleId,
                                   final DiscountType discountType,
                                   final BigDecimal discountValue,
                                   final boolean capApplied,
                                   final BigDecimal discountAmount,
                                   final String createdBy) {
        this.lineItem = lineItem;
        this.discountRuleId = discountRuleId;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.capApplied = capApplied;
        this.discountAmount = discountAmount;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public InvoiceLineItem getLineItem() {
        return lineItem;
    }

    public UUID getDiscountRuleId() {
        return discountRuleId;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public boolean isCapApplied() {
        return capApplied;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
