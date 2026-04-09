package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceCatalog;
import com.psyassistant.billing.catalog.ServiceStatus;
import com.psyassistant.scheduling.dto.SessionTypeResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record ServiceCatalogListItem(
        UUID id,
        String name,
        String category,
        SessionTypeResponse sessionType,
        int durationMin,
        ServiceStatus status,
        BigDecimal currentPrice
) {
    public static ServiceCatalogListItem from(final ServiceCatalog entity, final BigDecimal currentPrice) {
        SessionTypeResponse st = new SessionTypeResponse(
                entity.getSessionType().getId(),
                entity.getSessionType().getCode(),
                entity.getSessionType().getName(),
                entity.getSessionType().getDescription()
        );
        return new ServiceCatalogListItem(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                st,
                entity.getDurationMin(),
                entity.getStatus(),
                currentPrice
        );
    }
}
