package com.psyassistant.auth.token;

/**
 * Thrown when a one-time token is unknown, expired, already used or of the wrong purpose.
 */
public class InvalidTokenException extends RuntimeException {

    /** Why the token was rejected. */
    public enum Reason {
        /** Unknown, already used, or wrong purpose. */
        INVALID,
        /** Past its expiry. */
        EXPIRED
    }

    private final Reason reason;

    /**
     * Creates the exception.
     *
     * @param reason rejection reason
     */
    public InvalidTokenException(final Reason reason) {
        super("One-time token rejected: " + reason);
        this.reason = reason;
    }

    /**
     * Returns the rejection reason.
     *
     * @return reason
     */
    public Reason getReason() {
        return reason;
    }
}
