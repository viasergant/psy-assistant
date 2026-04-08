package com.psyassistant.billing.invoice.dto;

import java.math.BigDecimal;

/**
 * JPA native-query projection for the finance dashboard aggregates.
 *
 * <p>Column aliases in {@code InvoiceRepository#getDashboardSummary()} must match
 * the method names here (Spring Data maps by alias, case-insensitive).
 */
public interface DashboardSummary {

    BigDecimal getTotalOutstanding();

    BigDecimal getTotalOverdue();

    BigDecimal getCollectedThisMonth();

    BigDecimal getAging030();

    BigDecimal getAging3160();

    BigDecimal getAging60plus();
}
