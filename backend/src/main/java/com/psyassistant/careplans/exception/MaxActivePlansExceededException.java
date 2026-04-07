package com.psyassistant.careplans.exception;

/**
 * Thrown when the number of active care plans for a client would exceed the configured maximum.
 */
public class MaxActivePlansExceededException extends RuntimeException {

    public MaxActivePlansExceededException(final String message) {
        super(message);
    }
}
