package com.psyassistant.billing.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Response DTO for the finance dashboard endpoint. */
public record FinanceDashboardResponse(
        BigDecimal totalOutstandingAmount,
        BigDecimal totalOverdueAmount,
        BigDecimal collectedThisMonthAmount,
        AgingBuckets aging,
        Instant asOf
) { }
