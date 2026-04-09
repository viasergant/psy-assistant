package com.psyassistant.billing.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDefaultPriceRequest(
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull LocalDate effectiveFrom
) { }
