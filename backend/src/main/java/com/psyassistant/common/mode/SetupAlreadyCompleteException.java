package com.psyassistant.common.mode;

/**
 * Thrown when the clinic initial-admin setup endpoint is called but at least one
 * user already exists in the database.
 *
 * <p>Handled by {@link com.psyassistant.common.exception.GlobalExceptionHandler}
 * which returns HTTP 409 with code {@code SETUP_ALREADY_COMPLETE}.
 */
public class SetupAlreadyCompleteException extends RuntimeException {

    /**
     * Constructs the exception with a fixed message.
     */
    public SetupAlreadyCompleteException() {
        super("Initial setup already completed");
    }
}
