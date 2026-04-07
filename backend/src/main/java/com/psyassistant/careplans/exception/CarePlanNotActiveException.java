package com.psyassistant.careplans.exception;

/**
 * Thrown when a mutation is attempted on a care plan that is not in ACTIVE status.
 */
public class CarePlanNotActiveException extends RuntimeException {

    public CarePlanNotActiveException(final String message) {
        super(message);
    }
}
