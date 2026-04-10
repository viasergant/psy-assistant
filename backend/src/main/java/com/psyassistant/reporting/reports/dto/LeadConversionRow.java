package com.psyassistant.reporting.reports.dto;

import java.math.BigDecimal;

/**
 * A single row in the Lead Conversion Rate report.
 *
 * @param leadSource     the lead source label (e.g. REFERRAL, WEBSITE)
 * @param totalLeads     total leads created in the period from this source
 * @param convertedLeads number of those leads that were converted to clients
 * @param conversionRate conversion rate as a percentage (0–100), or null if no leads
 */
public record LeadConversionRow(
    String leadSource,
    long totalLeads,
    long convertedLeads,
    BigDecimal conversionRate
) {
}
