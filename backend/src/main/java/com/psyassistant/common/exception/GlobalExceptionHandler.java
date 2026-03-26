package com.psyassistant.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Catches all unhandled exceptions and returns a structured {@link ErrorResponse} JSON body.
 *
 * <p>No stack trace is included in the response body; stack traces are written to the
 * application log only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles every {@link Exception} not caught by a more specific handler.
     *
     * @param ex      the unhandled exception
     * @param request the current HTTP request (used to extract the request URI)
     * @return a 500 response containing an {@link ErrorResponse} payload
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            final Exception ex,
            final HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(
                new ErrorResponse(
                        Instant.now(),
                        500,
                        "Internal Server Error",
                        request.getRequestURI()
                )
        );
    }
}
