package com.psyassistant.billing.invoice;

/**
 * Lifecycle status of an {@link Invoice}.
 *
 * <p>Allowed transitions:
 * <pre>
 * DRAFT → ISSUED          (issue action)
 * DRAFT → CANCELLED       (cancel action)
 * ISSUED → PARTIALLY_PAID (partial payment)
 * ISSUED → PAID           (full payment)
 * ISSUED → OVERDUE        (nightly scheduler)
 * ISSUED → CANCELLED      (cancel action)
 * OVERDUE → PARTIALLY_PAID (partial payment)
 * OVERDUE → PAID          (full payment)
 * OVERDUE → CANCELLED     (cancel action)
 * PARTIALLY_PAID → PAID   (subsequent payment clears balance)
 * PARTIALLY_PAID → OVERDUE (nightly scheduler)
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    OVERDUE,
    PAID,
    PARTIALLY_PAID,
    CANCELLED
}
