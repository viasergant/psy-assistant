package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceCatalog;
import com.psyassistant.billing.catalog.ServiceStatus;
import com.psyassistant.billing.catalog.ServiceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceCatalogResponse(
        UUID id,
        String name,
        String category,
        ServiceType serviceType,
        int durationMin,
        ServiceStatus status,
        BigDecimal currentPrice,
        Instant createdAt,
        Instant updatedAt
) {
    public static ServiceCatalogResponse from(final ServiceCatalog entity, final BigDecimal currentPrice) {
        return new ServiceCatalogResponse(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getServiceType(),
                entity.getDurationMin(),
                entity.getStatus(),
                currentPrice,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
