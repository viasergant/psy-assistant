package com.psyassistant.billing.discount.dto;

import jakarta.validation.constraints.NotNull;

/** Request to toggle the active state of a discount rule. */
public record UpdateDiscountRuleStatusRequest(@NotNull Boolean active) {
}
