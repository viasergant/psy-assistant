package com.psyassistant.billing.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request body for appending a line item to a draft invoice. */
public record AddLineItemRequest(
    @NotBlank String description,
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) { }
