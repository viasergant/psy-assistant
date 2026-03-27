package com.psyassistant.common.exception;

import com.psyassistant.auth.service.AuthException;
import com.psyassistant.users.DuplicateEmailException;
import com.psyassistant.users.SelfDeactivationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles domain-level authentication failures (invalid credentials, expired token, etc.).
     *
     * @param ex      the authentication exception
     * @param request the current HTTP request
     * @return 401 with machine-readable code
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            final AuthException ex,
            final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        ex.getMessage(),
                        ex.getCode().name(),
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles Spring Security access-denied events (403).
     *
     * @param ex      the access denied exception
     * @param request the current HTTP request
     * @return 403 response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            final AccessDeniedException ex,
            final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.FORBIDDEN.value(),
                        "Access denied",
                        "FORBIDDEN",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles bean validation failures on request bodies.
     *
     * @param ex      the validation exception
     * @param request the current HTTP request
     * @return 400 response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            final MethodArgumentNotValidException ex,
            final HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        detail,
                        "VALIDATION_ERROR",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles attempts to create a user with an already-registered email address (409).
     *
     * @param ex      the duplicate email exception
     * @param request the current HTTP request
     * @return 409 Conflict with machine-readable code
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            final DuplicateEmailException ex,
            final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.CONFLICT.value(),
                        ex.getMessage(),
                        "DUPLICATE_EMAIL",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles attempts by an admin to deactivate their own account (400).
     *
     * @param ex      the self-deactivation exception
     * @param request the current HTTP request
     * @return 400 Bad Request
     */
    @ExceptionHandler(SelfDeactivationException.class)
    public ResponseEntity<ErrorResponse> handleSelfDeactivation(
            final SelfDeactivationException ex,
            final HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        "SELF_DEACTIVATION_FORBIDDEN",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles resource-not-found exceptions from the JPA layer (404).
     *
     * @param ex      the entity not found exception
     * @param request the current HTTP request
     * @return 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            final EntityNotFoundException ex,
            final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        "NOT_FOUND",
                        request.getRequestURI()
                )
        );
    }

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
        LOG.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
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
