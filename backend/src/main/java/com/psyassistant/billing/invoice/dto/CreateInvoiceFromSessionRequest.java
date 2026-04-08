package com.psyassistant.billing.invoice.dto;

import jakarta.validation.constraints.NotNull;

/** Request body for generating an invoice from a completed session. */
public record CreateInvoiceFromSessionRequest(
    @NotNull java.util.UUID sessionId,
    java.math.BigDecimal unitPriceOverride,
    String notes
) { }
