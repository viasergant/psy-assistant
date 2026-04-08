package com.psyassistant.billing.dashboard;

import com.psyassistant.billing.dashboard.dto.AgingBuckets;
import com.psyassistant.billing.dashboard.dto.FinanceDashboardResponse;
import com.psyassistant.billing.invoice.InvoiceRepository;
import com.psyassistant.billing.invoice.dto.DashboardSummary;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that assembles the finance dashboard aggregates.
 *
 * <p>Delegates the heavy aggregation to a single native SQL query in
 * {@link InvoiceRepository#getDashboardSummary()}.
 */
@Service
@Transactional(readOnly = true)
public class FinanceDashboardService {

    private final InvoiceRepository invoiceRepository;

    public FinanceDashboardService(final InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Returns a real-time snapshot of finance dashboard aggregates.
     *
     * @return dashboard response with outstanding, overdue, collected-this-month, and aging buckets
     */
    public FinanceDashboardResponse getDashboard() {
        DashboardSummary summary = invoiceRepository.getDashboardSummary();
        return new FinanceDashboardResponse(
                coalesce(summary.getTotalOutstanding()),
                coalesce(summary.getTotalOverdue()),
                coalesce(summary.getCollectedThisMonth()),
                new AgingBuckets(
                        coalesce(summary.getAging030()),
                        coalesce(summary.getAging3160()),
                        coalesce(summary.getAging60plus())),
                Instant.now()
        );
    }

    private static BigDecimal coalesce(final BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
