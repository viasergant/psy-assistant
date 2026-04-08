package com.psyassistant.billing.invoice;

/** Identifies which workflow created this invoice. */
public enum InvoiceSource {
    /** Generated from a completed billable session. */
    SESSION,
    /** Generated from a prepaid package purchase. */
    PACKAGE,
    /** Created manually by Finance Staff with free-text line items. */
    MANUAL
}
