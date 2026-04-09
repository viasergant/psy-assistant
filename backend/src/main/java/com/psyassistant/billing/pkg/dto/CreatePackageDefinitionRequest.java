package com.psyassistant.billing.pkg.dto;

import com.psyassistant.billing.catalog.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Request to create a new prepaid package definition. */
public record CreatePackageDefinitionRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull ServiceType serviceType,
    @Min(1) int sessionQty,
    @NotNull @DecimalMin("0.00") BigDecimal price
) { }
