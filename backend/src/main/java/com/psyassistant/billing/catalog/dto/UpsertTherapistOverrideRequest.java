package com.psyassistant.billing.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpsertTherapistOverrideRequest(
        @NotNull @DecimalMin("0.00") BigDecimal price
) { }
