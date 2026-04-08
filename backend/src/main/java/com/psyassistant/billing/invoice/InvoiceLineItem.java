package com.psyassistant.billing.invoice;

import com.psyassistant.common.audit.SimpleBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A single line item inside an {@link Invoice}.
 *
 * <p>Line totals must always equal {@code quantity × unitPrice}.
 * Monetary values use {@link BigDecimal}.
 */
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem extends SimpleBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // ---- constructors ----

    protected InvoiceLineItem() { }

    public InvoiceLineItem(final String description,
                           final BigDecimal quantity,
                           final BigDecimal unitPrice,
                           final int sortOrder) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity.multiply(unitPrice);
        this.sortOrder = sortOrder;
    }

    // ---- getters/setters ----

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(final Invoice invoice) {
        this.invoice = invoice;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
