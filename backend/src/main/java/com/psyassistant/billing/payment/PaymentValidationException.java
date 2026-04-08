package com.psyassistant.billing.payment;

/**
 * Thrown when a payment amount is invalid for the target invoice —
 * e.g., the amount exceeds the outstanding balance.
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity by
 * {@link com.psyassistant.common.exception.GlobalExceptionHandler}.
 */
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(final String message) {
        super(message);
    }
}
