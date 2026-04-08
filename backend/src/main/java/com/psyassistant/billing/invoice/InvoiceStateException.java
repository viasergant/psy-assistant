package com.psyassistant.billing.invoice;

/**
 * Thrown when an attempted state transition on an {@link Invoice} is not permitted
 * by the invoice state machine, or when a mutation is attempted on a locked invoice.
 */
public class InvoiceStateException extends RuntimeException {

    public InvoiceStateException(final String message) {
        super(message);
    }
}
