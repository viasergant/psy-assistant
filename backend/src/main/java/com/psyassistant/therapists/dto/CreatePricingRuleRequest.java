package com.psyassistant.therapists.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for creating a therapist pricing rule.
 *
 * <p>{@code sessionTypeId} must be the UUID of an active session type from
 * {@code GET /api/v1/appointments/session-types}.
 */
public record CreatePricingRuleRequest(
        @NotNull UUID sessionTypeId,
        @NotNull @DecimalMin("0.00") BigDecimal rate,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate effectiveFrom
) { }
