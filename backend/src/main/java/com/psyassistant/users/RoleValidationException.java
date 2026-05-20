package com.psyassistant.users;

/**
 * Thrown when a request violates a role-based business rule, such as calling a
 * therapist-specific endpoint without including {@code THERAPIST} in the roles set.
 *
 * <p>Maps to HTTP 400 Bad Request via
 * {@link com.psyassistant.common.exception.GlobalExceptionHandler}.
 */
public class RoleValidationException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message.
     *
     * @param message human-readable description of the violation
     */
    public RoleValidationException(final String message) {
        super(message);
    }
}
