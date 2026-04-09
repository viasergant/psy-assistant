package com.psyassistant.billing.pkg.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Request to sell a prepaid package to a client. */
public record SellPackageRequest(
    @NotNull UUID definitionId,
    @NotNull UUID clientId,
    @NotNull Instant purchasedAt,
    @Min(0) Integer validityDays
) { }
