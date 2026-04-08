package com.psyassistant.billing.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Request body for creating a manual invoice with free-text line items. */
public record CreateManualInvoiceRequest(
    @NotNull UUID clientId,
    UUID therapistId,
    @NotEmpty @Valid List<LineItemRequest> lineItems,
    BigDecimal discount,
    String notes
) {
    /** Embedded line item for manual invoice creation. */
    public record LineItemRequest(
        @NotNull String description,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal unitPrice
    ) { }
}
