package com.psyassistant.billing.invoice;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice entity representing a billing document.
 *
 * <p>State machine (see {@link InvoiceStatus}):
 * <pre>
 * DRAFT → ISSUED → PAID
 *      ↘         ↘ OVERDUE → PAID
 *       CANCELLED ↙        ↘ CANCELLED
 * </pre>
 *
 * <p>Monetary fields use {@link BigDecimal} to avoid floating-point rounding errors.
 */
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 20)
    private String invoiceNumber;

    @Column(name = "invoice_year", nullable = false)
    private short invoiceYear;

    @Column(name = "invoice_seq", nullable = false)
    private int invoiceSeq;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "therapist_id")
    private UUID therapistId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20, updatable = false)
    private InvoiceSource source;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "prepaid_package_id")
    private UUID prepaidPackageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "updated_by")
    private String updatedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("sort_order ASC, created_at ASC")
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    // ---- constructors ----

    protected Invoice() { }

    public Invoice(final UUID clientId, final UUID therapistId, final InvoiceSource source) {
        this.clientId = clientId;
        this.therapistId = therapistId;
        this.source = source;
    }

    // ---- business methods ----

    /**
     * Locks the invoice, sets issued date, and transitions to ISSUED state.
     * Throws {@link InvoiceStateException} if not in DRAFT state.
     *
     * @param issuedDate the date of issue
     * @param dueDate    the payment due date
     */
    public void issue(final LocalDate issuedDate, final LocalDate dueDate) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new InvoiceStateException(
                    "Invoice can only be issued from DRAFT state, current: " + this.status);
        }
        if (this.lineItems.isEmpty()) {
            throw new InvoiceStateException("Invoice must have at least one line item before issuing");
        }
        this.status = InvoiceStatus.ISSUED;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
    }

    /**
     * Cancels the invoice with a mandatory reason.
     *
     * @param reason the cancellation reason (required)
     */
    public void cancel(final String reason) {
        if (this.status == InvoiceStatus.PAID || this.status == InvoiceStatus.CANCELLED) {
            throw new InvoiceStateException(
                    "Invoice in state " + this.status + " cannot be cancelled");
        }
        this.status = InvoiceStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
    }

    /**
     * Marks the invoice as overdue (called by the nightly scheduler).
     * Only valid from ISSUED state.
     */
    public void markOverdue() {
        if (this.status == InvoiceStatus.ISSUED) {
            this.status = InvoiceStatus.OVERDUE;
        }
    }

    /**
     * Adds a line item and recalculates subtotal and total.
     * Only allowed for DRAFT invoices.
     *
     * @param item the line item to add
     */
    public void addLineItem(final InvoiceLineItem item) {
        assertEditable();
        item.setInvoice(this);
        this.lineItems.add(item);
        recalculate();
    }

    /**
     * Removes a line item by id and recalculates totals.
     * Only allowed for DRAFT invoices.
     *
     * @param itemId the UUID of the item to remove
     */
    public void removeLineItem(final UUID itemId) {
        assertEditable();
        boolean removed = this.lineItems.removeIf(i -> itemId.equals(i.getId()));
        if (!removed) {
            throw new jakarta.persistence.EntityNotFoundException("Line item not found: " + itemId);
        }
        recalculate();
    }

    private void assertEditable() {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new InvoiceStateException("Invoice is locked and cannot be edited");
        }
    }

    private void recalculate() {
        this.subtotal = lineItems.stream()
                .map(InvoiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.subtract(this.discount);
    }

    // ---- getters/setters ----

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(final String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public short getInvoiceYear() {
        return invoiceYear;
    }

    public void setInvoiceYear(final short invoiceYear) {
        this.invoiceYear = invoiceYear;
    }

    public int getInvoiceSeq() {
        return invoiceSeq;
    }

    public void setInvoiceSeq(final int invoiceSeq) {
        this.invoiceSeq = invoiceSeq;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public InvoiceSource getSource() {
        return source;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(final UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getPrepaidPackageId() {
        return prepaidPackageId;
    }

    public void setPrepaidPackageId(final UUID prepaidPackageId) {
        this.prepaidPackageId = prepaidPackageId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(final BigDecimal discount) {
        this.discount = discount;
        recalculate();
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(final String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public List<InvoiceLineItem> getLineItems() {
        return lineItems;
    }
}
