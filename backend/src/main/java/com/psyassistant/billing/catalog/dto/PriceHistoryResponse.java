package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceCatalogPriceHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PriceHistoryResponse(
        UUID id,
        BigDecimal price,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String changedBy
) {
    public static PriceHistoryResponse from(final ServiceCatalogPriceHistory entity) {
        return new PriceHistoryResponse(
                entity.getId(),
                entity.getPrice(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getChangedBy()
        );
    }
}
