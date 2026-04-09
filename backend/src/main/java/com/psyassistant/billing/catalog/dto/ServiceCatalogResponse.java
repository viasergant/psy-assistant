package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceCatalog;
import com.psyassistant.billing.catalog.ServiceStatus;
import com.psyassistant.scheduling.dto.SessionTypeResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceCatalogResponse(
        UUID id,
        String name,
        String category,
        SessionTypeResponse sessionType,
        int durationMin,
        ServiceStatus status,
        BigDecimal currentPrice,
        Instant createdAt,
        Instant updatedAt
) {
    public static ServiceCatalogResponse from(final ServiceCatalog entity, final BigDecimal currentPrice) {
        SessionTypeResponse st = new SessionTypeResponse(
                entity.getSessionType().getId(),
                entity.getSessionType().getCode(),
                entity.getSessionType().getName(),
                entity.getSessionType().getDescription()
        );
        return new ServiceCatalogResponse(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                st,
                entity.getDurationMin(),
                entity.getStatus(),
                currentPrice,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
