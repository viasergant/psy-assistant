package com.psyassistant.billing.invoice.scheduler;

import com.psyassistant.billing.invoice.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nightly scheduled job that transitions Issued invoices past their due date
 * to Overdue status.
 *
 * <p>Runs at 01:00 every night (server local time).
 * The bulk-update query in {@link InvoiceRepository#bulkMarkOverdue()} compares
 * {@code due_date < CURRENT_DATE} so only invoices whose due date has fully
 * passed are transitioned.
 */
@Component
public class OverdueInvoiceScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueInvoiceScheduler.class);

    private final InvoiceRepository invoiceRepository;

    public OverdueInvoiceScheduler(final InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Marks all issued invoices with a past due date as Overdue.
     *
     * <p>Cron expression {@code "0 0 1 * * *"} triggers at 01:00:00 every day.
     * The job is idempotent: re-running it on already-overdue invoices has no effect.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueInvoices() {
        LOG.info("Overdue invoice detection job started");
        int updated = invoiceRepository.bulkMarkOverdue();
        LOG.info("Overdue invoice detection job completed — {} invoice(s) marked overdue", updated);
    }
}
