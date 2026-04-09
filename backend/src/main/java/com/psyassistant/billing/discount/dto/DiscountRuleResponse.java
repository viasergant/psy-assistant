package com.psyassistant.billing.discount.dto;

import com.psyassistant.billing.discount.DiscountRule;
import com.psyassistant.billing.discount.DiscountScope;
import com.psyassistant.billing.discount.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response DTO for a discount rule. */
public record DiscountRuleResponse(
    UUID id,
    String name,
    DiscountType type,
    BigDecimal value,
    DiscountScope scope,
    UUID clientId,
    UUID serviceCatalogId,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static DiscountRuleResponse from(final DiscountRule rule) {
        return new DiscountRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getType(),
                rule.getValue(),
                rule.getScope(),
                rule.getClientId(),
                rule.getServiceCatalogId(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
