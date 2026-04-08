package com.psyassistant.billing.dashboard.dto;

import java.math.BigDecimal;

/** Receivables aging buckets for the finance dashboard. */
public record AgingBuckets(
        BigDecimal current030,
        BigDecimal past3160,
        BigDecimal past60plus
) { }
