package com.psyassistant.reporting.reports.dto;

import java.math.BigDecimal;

/**
 * A single-row result for the Client Retention report (period-level aggregate).
 *
 * @param activeAtEndOfPeriod number of distinct clients active in the period
 * @param newAcquired         clients with their first session inside the period
 * @param churned             clients whose last session predates the churn threshold
 * @param retentionRate       retention rate as a percentage (null if denominator is zero)
 */
public record ClientRetentionRow(
    long activeAtEndOfPeriod,
    long newAcquired,
    long churned,
    BigDecimal retentionRate
) {
}
