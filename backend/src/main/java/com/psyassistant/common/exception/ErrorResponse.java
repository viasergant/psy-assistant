package com.psyassistant.common.exception;

import java.time.Instant;

/**
 * Structured error payload returned by {@link GlobalExceptionHandler}.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     short human-readable description
 * @param code      machine-readable error code (e.g. INVALID_CREDENTIALS); may be null
 * @param path      request URI that triggered the error
 */
public record ErrorResponse(Instant timestamp, int status, String error,
                            String code, String path) {

    /**
     * Convenience constructor without a machine-readable code.
     *
     * @param timestamp when the error occurred
     * @param status    HTTP status code
     * @param error     short human-readable description
     * @param path      request URI that triggered the error
     */
    public ErrorResponse(final Instant timestamp, final int status,
                         final String error, final String path) {
        this(timestamp, status, error, null, path);
    }
}
