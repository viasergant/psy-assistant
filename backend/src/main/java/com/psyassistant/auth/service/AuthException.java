package com.psyassistant.auth.service;

/**
 * Thrown by {@link AuthService} for domain-level authentication failures.
 *
 * <p>Each instance carries a machine-readable {@link ErrorCode} used by the
 * global exception handler to populate the {@code code} field in the error response.
 */
public class AuthException extends RuntimeException {

    /**
     * Machine-readable error codes returned in the API response body.
     */
    public enum ErrorCode {

        /** Supplied credentials (email / password) do not match. */
        INVALID_CREDENTIALS,

        /** The user account exists but has been deactivated. */
        ACCOUNT_DISABLED,

        /** The supplied refresh token is expired, revoked, or unknown. */
        TOKEN_EXPIRED,

        /** The account has been temporarily locked due to too many failed login attempts. */
        ACCOUNT_LOCKED,

        /** The supplied password-reset token is invalid or has already been used. */
        TOKEN_INVALID
    }

    private final ErrorCode code;

    /**
     * Constructs an AuthException with the given error code and message.
     *
     * @param code    machine-readable error code
     * @param message human-readable description (for logging only — not returned to client)
     */
    public AuthException(final ErrorCode code, final String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the machine-readable error code.
     *
     * @return error code
     */
    public ErrorCode getCode() {
        return code;
    }
}
