package com.psyassistant.auth.service;

import java.util.List;

/**
 * Thrown when a supplied password does not meet the configured complexity policy.
 *
 * <p>Each instance carries a list of machine-readable violation codes (e.g.
 * {@code MIN_LENGTH}, {@code REQUIRES_UPPERCASE}) that the global exception
 * handler includes in the HTTP 400 response body.
 */
public class PasswordPolicyException extends RuntimeException {

    private final List<String> violations;

    /**
     * Constructs the exception with a list of policy violations.
     *
     * @param violations non-empty list of violation codes
     */
    public PasswordPolicyException(final List<String> violations) {
        super("Password does not meet policy requirements: " + violations);
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the list of constraint violation codes.
     *
     * @return immutable list of violation codes
     */
    public List<String> getViolations() {
        return violations;
    }
}
