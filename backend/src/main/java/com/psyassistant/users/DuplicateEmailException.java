package com.psyassistant.users;

/**
 * Thrown when an attempt is made to create a user with an email that is already registered.
 *
 * <p>Maps to HTTP 409 Conflict via {@link com.psyassistant.common.exception.GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message.
     *
     * @param email the duplicate email address
     */
    public DuplicateEmailException(final String email) {
        super("Email address is already registered: " + email);
    }
}
