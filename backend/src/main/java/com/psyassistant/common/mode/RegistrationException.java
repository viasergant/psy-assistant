package com.psyassistant.common.mode;

/**
 * Thrown when self-service registration cannot proceed, for example because
 * the email address is already registered.
 */
public class RegistrationException extends RuntimeException {

    public RegistrationException(final String message) {
        super(message);
    }
}
