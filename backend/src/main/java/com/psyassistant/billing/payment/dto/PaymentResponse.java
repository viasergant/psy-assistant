package com.psyassistant.billing.payment.dto;

import com.psyassistant.billing.invoice.InvoiceStatus;
import com.psyassistant.billing.payment.Payment;
import com.psyassistant.billing.payment.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Response DTO representing a registered payment. */
public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        LocalDate paymentDate,
        String reference,
        String notes,
        String createdBy,
        Instant createdAt,
        BigDecimal invoiceOutstandingBalance,
        InvoiceStatus invoiceStatus
) {

    /** Factory: builds from entity plus contextual invoice state. */
    public static PaymentResponse from(
            final Payment payment,
            final BigDecimal invoiceOutstandingBalance,
            final InvoiceStatus invoiceStatus) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentDate(),
                payment.getReference(),
                payment.getNotes(),
                payment.getCreatedBy(),
                payment.getCreatedAt(),
                invoiceOutstandingBalance,
                invoiceStatus
        );
    }
}
