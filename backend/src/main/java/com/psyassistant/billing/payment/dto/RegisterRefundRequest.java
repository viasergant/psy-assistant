package com.psyassistant.billing.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Request body for registering a refund against a paid invoice. */
public record RegisterRefundRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Refund reason is required")
        String reason,

        @NotNull(message = "Refund date is required")
        LocalDate refundDate,

        /** Optional: UUID of the specific payment this refund relates to. */
        UUID paymentId,

        @Size(max = 255, message = "Reference must not exceed 255 characters")
        String reference
) { }
