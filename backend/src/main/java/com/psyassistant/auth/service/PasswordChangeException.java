package com.psyassistant.auth.service;

/**
 * Thrown when an authenticated user's self-service password change is rejected.
 *
 * <p>Deliberately separate from {@link AuthException}: that one maps to HTTP 401, which makes
 * the frontend interceptor try a token refresh. The caller here is already authenticated.
 */
public class PasswordChangeException extends RuntimeException {

    /** Machine-readable reasons returned in the API response body. */
    public enum Reason {

        /** The supplied current password does not match. */
        INVALID_CURRENT_PASSWORD,

        /** The new password equals the current one. */
        SAME_PASSWORD,

        /** Too many wrong current-password attempts; the account is temporarily locked. */
        ACCOUNT_LOCKED
    }

    private final Reason reason;

    /**
     * Constructs the exception.
     *
     * @param reason  machine-readable reason
     * @param message human-readable description
     */
    public PasswordChangeException(final Reason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * Returns the machine-readable reason.
     *
     * @return the reason
     */
    public Reason getReason() {
        return reason;
    }
}
