package com.psyassistant.billing.invoice.dto;

import com.psyassistant.billing.invoice.Invoice;
import com.psyassistant.billing.invoice.InvoiceLineItem;
import com.psyassistant.billing.invoice.InvoiceSource;
import com.psyassistant.billing.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full invoice detail response (includes line items). */
public record InvoiceResponse(
    UUID id,
    String invoiceNumber,
    UUID clientId,
    UUID therapistId,
    InvoiceSource source,
    UUID sessionId,
    UUID prepaidPackageId,
    InvoiceStatus status,
    LocalDate issuedDate,
    LocalDate dueDate,
    String cancellationReason,
    Instant cancelledAt,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String notes,
    String pdfPath,
    List<LineItemResponse> lineItems,
    Instant createdAt,
    Instant updatedAt
) {
    /** Line item projection. */
    public record LineItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int sortOrder
    ) { }

    /** Factory method to build from the JPA entity. */
    public static InvoiceResponse from(final Invoice invoice) {
        List<LineItemResponse> items = invoice.getLineItems().stream()
                .map(InvoiceResponse::lineItemFrom)
                .toList();
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getClientId(),
                invoice.getTherapistId(),
                invoice.getSource(),
                invoice.getSessionId(),
                invoice.getPrepaidPackageId(),
                invoice.getStatus(),
                invoice.getIssuedDate(),
                invoice.getDueDate(),
                invoice.getCancellationReason(),
                invoice.getCancelledAt(),
                invoice.getSubtotal(),
                invoice.getDiscount(),
                invoice.getTotal(),
                invoice.getNotes(),
                invoice.getPdfPath(),
                items,
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    private static LineItemResponse lineItemFrom(final InvoiceLineItem item) {
        return new LineItemResponse(
                item.getId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getSortOrder()
        );
    }
}
