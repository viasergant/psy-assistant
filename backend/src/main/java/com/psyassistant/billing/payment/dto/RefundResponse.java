package com.psyassistant.billing.payment.dto;

import com.psyassistant.billing.payment.Refund;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Response DTO representing a registered refund. */
public record RefundResponse(
        UUID id,
        UUID invoiceId,
        UUID paymentId,
        BigDecimal amount,
        String reason,
        LocalDate refundDate,
        String reference,
        String createdBy,
        Instant createdAt
) {

    /** Factory: builds from entity. */
    public static RefundResponse from(final Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getInvoiceId(),
                refund.getPaymentId(),
                refund.getAmount(),
                refund.getReason(),
                refund.getRefundDate(),
                refund.getReference(),
                refund.getCreatedBy(),
                refund.getCreatedAt()
        );
    }
}
