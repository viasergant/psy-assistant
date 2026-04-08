package com.psyassistant.billing.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tracks the last invoice sequence number for each calendar year.
 * Used with {@code SELECT ... FOR UPDATE} to generate race-safe invoice numbers.
 */
@Entity
@Table(name = "invoice_number_seq")
public class InvoiceNumberSeq {

    @Id
    @Column(name = "invoice_year")
    private short invoiceYear;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq;

    protected InvoiceNumberSeq() { }

    public InvoiceNumberSeq(final short invoiceYear) {
        this.invoiceYear = invoiceYear;
        this.lastSeq = 0;
    }

    public short getInvoiceYear() {
        return invoiceYear;
    }

    public int getLastSeq() {
        return lastSeq;
    }

    public void setLastSeq(final int lastSeq) {
        this.lastSeq = lastSeq;
    }
}
