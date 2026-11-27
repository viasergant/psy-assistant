package com.psyassistant.users;

/**
 * Thrown when a user cannot be permanently deleted because clinical, financial or audit
 * records still reference the account.
 *
 * <p>Maps to HTTP 409 Conflict via {@link com.psyassistant.common.exception.GlobalExceptionHandler}.
 */
public class UserHasHistoryException extends RuntimeException {

    /** Constructs the exception with a fixed message. */
    public UserHasHistoryException() {
        super("User has related records and cannot be deleted; suspend the account instead");
    }
}
