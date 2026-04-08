package com.psyassistant.billing.invoice.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for cancelling an invoice. */
public record CancelInvoiceRequest(
    @NotBlank String reason
) { }
