package com.psyassistant.billing.pkg.dto;

import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.billing.pkg.PackageDefinitionStatus;
import com.psyassistant.billing.pkg.PrepaidPackageDefinition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** Response DTO for a prepaid package definition. */
public record PackageDefinitionResponse(
    UUID id,
    String name,
    ServiceType serviceType,
    int sessionQty,
    BigDecimal price,
    BigDecimal perSessionDisplay,
    PackageDefinitionStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static PackageDefinitionResponse from(final PrepaidPackageDefinition def) {
        BigDecimal perSession = def.getSessionQty() > 0
                ? def.getPrice().divide(BigDecimal.valueOf(def.getSessionQty()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PackageDefinitionResponse(
                def.getId(),
                def.getName(),
                def.getServiceType(),
                def.getSessionQty(),
                def.getPrice(),
                perSession,
                def.getStatus(),
                def.getCreatedAt(),
                def.getUpdatedAt()
        );
    }
}
