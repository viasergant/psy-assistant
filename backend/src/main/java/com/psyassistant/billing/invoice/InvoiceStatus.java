package com.psyassistant.billing.invoice;

/**
 * Lifecycle status of an {@link Invoice}.
 *
 * <p>Allowed transitions:
 * <pre>
 * DRAFT → ISSUED     (issue action)
 * DRAFT → CANCELLED  (cancel action)
 * ISSUED → PAID      (record payment — stub this release)
 * ISSUED → OVERDUE   (nightly scheduler)
 * ISSUED → CANCELLED (cancel action)
 * OVERDUE → PAID     (record payment — stub)
 * OVERDUE → CANCELLED (cancel action)
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    OVERDUE,
    PAID,
    CANCELLED
}
