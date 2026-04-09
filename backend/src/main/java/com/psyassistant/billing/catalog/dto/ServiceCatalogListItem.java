package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceCatalog;
import com.psyassistant.billing.catalog.ServiceStatus;
import com.psyassistant.billing.catalog.ServiceType;
import java.math.BigDecimal;
import java.util.UUID;

public record ServiceCatalogListItem(
        UUID id,
        String name,
        String category,
        ServiceType serviceType,
        int durationMin,
        ServiceStatus status,
        BigDecimal currentPrice
) {
    public static ServiceCatalogListItem from(final ServiceCatalog entity, final BigDecimal currentPrice) {
        return new ServiceCatalogListItem(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getServiceType(),
                entity.getDurationMin(),
                entity.getStatus(),
                currentPrice
        );
    }
}
