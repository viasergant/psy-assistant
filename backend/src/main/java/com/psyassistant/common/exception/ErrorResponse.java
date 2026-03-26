package com.psyassistant.common.exception;

import java.time.Instant;

/**
 * Structured error payload returned by {@link GlobalExceptionHandler}.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     short human-readable description
 * @param path      request URI that triggered the error
 */
public record ErrorResponse(Instant timestamp, int status, String error, String path) {
}
