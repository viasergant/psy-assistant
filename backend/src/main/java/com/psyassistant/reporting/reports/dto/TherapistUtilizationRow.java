package com.psyassistant.reporting.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single row in the Therapist Utilization report.
 *
 * @param therapistProfileId UUID of the therapist profile record
 * @param therapistName      full name of the therapist
 * @param bookedMinutes      total non-cancelled appointment minutes in the period
 * @param availableMinutes   contracted capacity in minutes for the period (null if not set)
 * @param utilizationPct     utilization percentage (null if availableMinutes is null or zero)
 */
public record TherapistUtilizationRow(
    UUID therapistProfileId,
    String therapistName,
    long bookedMinutes,
    Long availableMinutes,
    BigDecimal utilizationPct
) {
}
