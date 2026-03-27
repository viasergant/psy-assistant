package com.psyassistant.users;

/**
 * Thrown when a System Administrator attempts to deactivate their own account.
 *
 * <p>Maps to HTTP 400 Bad Request via {@link com.psyassistant.common.exception.GlobalExceptionHandler}.
 */
public class SelfDeactivationException extends RuntimeException {

    /** Constructs the exception with a fixed message. */
    public SelfDeactivationException() {
        super("Admin cannot deactivate own account");
    }
}
