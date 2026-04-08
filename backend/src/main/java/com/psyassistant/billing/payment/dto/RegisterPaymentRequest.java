package com.psyassistant.billing.payment.dto;

import com.psyassistant.billing.payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for recording a payment against an invoice. */
public record RegisterPaymentRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Payment date is required")
        LocalDate paymentDate,

        @Size(max = 255, message = "Reference must not exceed 255 characters")
        String reference,

        String notes
) { }
