package com.psyassistant.billing.invoice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request body for creating an invoice from a prepaid package (stub until PA-49). */
public record CreatePackageInvoiceRequest(
    @NotNull UUID prepaidPackageId
) { }
