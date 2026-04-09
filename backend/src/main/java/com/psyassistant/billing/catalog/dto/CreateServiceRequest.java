package com.psyassistant.billing.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateServiceRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String category,
        @NotNull UUID sessionTypeId,
        @Min(1) int durationMin,
        @NotNull @DecimalMin("0.00") BigDecimal defaultPrice,
        @NotNull LocalDate effectiveFrom
) { }
