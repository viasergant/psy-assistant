package com.psyassistant.reporting.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single row in the Revenue report.
 *
 * @param month             first day of the month for calendar grouping
 * @param therapistId       UUID of the therapist profile
 * @param therapistName     full name of the therapist
 * @param invoicedTotal     sum of invoice subtotals issued in the month
 * @param paidTotal         sum of payments received against those invoices
 * @param outstandingAmount invoicedTotal minus paidTotal
 */
public record RevenueRow(
    LocalDate month,
    UUID therapistId,
    String therapistName,
    BigDecimal invoicedTotal,
    BigDecimal paidTotal,
    BigDecimal outstandingAmount
) {
}
