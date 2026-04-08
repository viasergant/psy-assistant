package com.psyassistant.billing.invoice.dto;

import com.psyassistant.billing.invoice.Invoice;
import com.psyassistant.billing.invoice.InvoiceSource;
import com.psyassistant.billing.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Lightweight invoice summary for list views (no line items). */
public record InvoiceListItemResponse(
    UUID id,
    String invoiceNumber,
    UUID clientId,
    UUID therapistId,
    InvoiceSource source,
    InvoiceStatus status,
    LocalDate issuedDate,
    LocalDate dueDate,
    BigDecimal total,
    Instant createdAt
) {
    /** Factory method. */
    public static InvoiceListItemResponse from(final Invoice invoice) {
        return new InvoiceListItemResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getClientId(),
                invoice.getTherapistId(),
                invoice.getSource(),
                invoice.getStatus(),
                invoice.getIssuedDate(),
                invoice.getDueDate(),
                invoice.getTotal(),
                invoice.getCreatedAt()
        );
    }
}
