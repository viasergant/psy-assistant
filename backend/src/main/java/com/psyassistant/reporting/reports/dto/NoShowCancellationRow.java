package com.psyassistant.reporting.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single row in the No-Show and Cancellation Rate report.
 *
 * @param therapistId        UUID of the therapist profile
 * @param therapistName      full name of the therapist
 * @param totalScheduled     total appointments in the period
 * @param noShowCount        appointments with status NO_SHOW
 * @param noShowRate         no-show rate as a percentage
 * @param cancellationCount  appointments with status CANCELLED
 * @param cancellationRate   cancellation rate as a percentage
 */
public record NoShowCancellationRow(
    UUID therapistId,
    String therapistName,
    long totalScheduled,
    long noShowCount,
    BigDecimal noShowRate,
    long cancellationCount,
    BigDecimal cancellationRate
) {
}
