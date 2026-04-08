package com.psyassistant.billing.wallet;

/**
 * Type of transaction in a client credit wallet.
 */
public enum WalletTransactionType {
    OVERPAYMENT_CREDIT,
    INVOICE_DEBIT,
    REFUND_CREDIT,
    MANUAL_ADJUSTMENT
}
