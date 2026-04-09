package com.psyassistant.billing.pkg.dto;

import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.billing.pkg.PackageInstanceStatus;
import com.psyassistant.billing.pkg.PrepaidPackageInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Response DTO for a sold prepaid package instance. */
public record PackageInstanceResponse(
    UUID id,
    UUID definitionId,
    String definitionName,
    ServiceType serviceType,
    BigDecimal packagePrice,
    UUID clientId,
    Instant purchasedAt,
    UUID invoiceId,
    int sessionsRemaining,
    int sessionsTotal,
    PackageInstanceStatus status,
    LocalDate expiresAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static PackageInstanceResponse from(final PrepaidPackageInstance inst) {
        return new PackageInstanceResponse(
                inst.getId(),
                inst.getDefinition().getId(),
                inst.getDefinition().getName(),
                inst.getDefinition().getServiceType(),
                inst.getDefinition().getPrice(),
                inst.getClientId(),
                inst.getPurchasedAt(),
                inst.getInvoiceId(),
                inst.getSessionsRemaining(),
                inst.getSessionsTotal(),
                inst.getStatus(),
                inst.getExpiresAt(),
                inst.getCreatedAt(),
                inst.getUpdatedAt()
        );
    }
}
