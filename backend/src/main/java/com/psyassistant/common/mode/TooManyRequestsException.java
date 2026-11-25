package com.psyassistant.common.mode;

/**
 * Thrown when a public endpoint is called more often than allowed.
 */
public class TooManyRequestsException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message description for logs
     */
    public TooManyRequestsException(final String message) {
        super(message);
    }
}
