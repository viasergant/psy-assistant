package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceCatalogTherapistOverride;
import java.math.BigDecimal;
import java.util.UUID;

public record TherapistOverrideResponse(
        UUID id,
        UUID therapistId,
        String therapistName,
        BigDecimal price
) {
    public static TherapistOverrideResponse from(final ServiceCatalogTherapistOverride entity,
                                                  final String therapistName) {
        return new TherapistOverrideResponse(
                entity.getId(),
                entity.getTherapistId(),
                therapistName,
                entity.getPrice()
        );
    }
}
