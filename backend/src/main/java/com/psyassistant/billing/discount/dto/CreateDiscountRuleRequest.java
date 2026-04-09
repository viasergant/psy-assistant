package com.psyassistant.billing.discount.dto;

import com.psyassistant.billing.discount.DiscountScope;
import com.psyassistant.billing.discount.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Request to create a new discount rule. */
public record CreateDiscountRuleRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull DiscountType type,
    @NotNull @DecimalMin("0.01") BigDecimal value,
    @NotNull DiscountScope scope,
    UUID clientId,
    UUID serviceCatalogId
) { }
