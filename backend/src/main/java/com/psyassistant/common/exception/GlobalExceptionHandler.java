package com.psyassistant.common.exception;

import com.psyassistant.auth.service.AuthException;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.crm.leads.InvalidStatusTransitionException;
import com.psyassistant.crm.leads.LeadAlreadyConvertedException;
import com.psyassistant.users.DuplicateEmailException;
import com.psyassistant.users.SelfDeactivationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Catches all unhandled exceptions and returns a structured {@link ErrorResponse} JSON body.
 *
 * <p>No stack trace is included in the response body; stack traces are written to the
 * application log only.
 *
 * <p>Every {@link AccessDeniedException} (HTTP 403) is recorded to the audit log
 * with event type {@code ACCESS_DENIED}, the authenticated user's ID, and the
 * requested URI before the error response is returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ACCESS_DENIED_EVENT = "ACCESS_DENIED";

    private final AuditLogService auditLogService;

    /**
     * Constructs the handler with the required audit log service.
     *
     * @param auditLogService service for persisting security event records
     */
    public GlobalExceptionHandler(final AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

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
     * <p>Records an {@code ACCESS_DENIED} audit log entry with the authenticated
     * user's ID and the requested URI before returning the 403 response.
     *
     * @param ex      the access denied exception
     * @param request the current HTTP request
     * @return 403 response with code {@code ACCESS_DENIED}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            final AccessDeniedException ex,
            final HttpServletRequest request) {

        UUID userId = extractUserId();
        recordAccessDeniedAuditLog(userId, request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.FORBIDDEN.value(),
                        "Access denied",
                        ACCESS_DENIED_EVENT,
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
     * Handles attempts to convert a lead that has already been converted (409).
     *
     * <p>Returns a {@link ConversionErrorResponse} that includes the {@code existingClientId}
     * so the caller can navigate to the existing client record.
     *
     * @param ex      the already-converted exception
     * @param request the current HTTP request
     * @return 409 Conflict with machine-readable code and optional existingClientId
     */
    @ExceptionHandler(LeadAlreadyConvertedException.class)
    public ResponseEntity<ConversionErrorResponse> handleLeadAlreadyConverted(
            final LeadAlreadyConvertedException ex,
            final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ConversionErrorResponse(
                        Instant.now(),
                        HttpStatus.CONFLICT.value(),
                        ex.getMessage(),
                        "LEAD_ALREADY_CONVERTED",
                        request.getRequestURI(),
                        ex.getExistingClientId()
                )
        );
    }

    /**
     * Handles invalid lead status transition attempts (422).
     *
     * @param ex      the invalid transition exception
     * @param request the current HTTP request
     * @return 422 Unprocessable Entity with machine-readable code
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
            final InvalidStatusTransitionException ex,
            final HttpServletRequest request) {
        return ResponseEntity.unprocessableEntity().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        ex.getMessage(),
                        "INVALID_STATUS_TRANSITION",
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
         * Handles optimistic locking conflicts on stale updates (409).
         */
        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleOptimisticLock(
                        final ObjectOptimisticLockingFailureException ex,
                        final HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                new ErrorResponse(
                                                Instant.now(),
                                                HttpStatus.CONFLICT.value(),
                                                "Resource was updated by another request. Refresh and retry.",
                                                "STALE_UPDATE",
                                                request.getRequestURI()
                                )
                );
        }

    /**
     * Handles type conversion failures on path/query parameters (400).
     *
     * <p>Triggered when a request parameter cannot be converted to the expected type,
     * such as passing an invalid UUID string where a UUID is expected.
     *
     * @param ex      the type mismatch exception
     * @param request the current HTTP request
     * @return 400 Bad Request with clear error message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            final MethodArgumentTypeMismatchException ex,
            final HttpServletRequest request) {
        String message = String.format("Invalid value for parameter '%s': %s",
                ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        message,
                        "INVALID_PARAMETER",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles malformed JSON requests or unrecognized fields (400).
     *
     * <p>Triggered when the request body cannot be parsed or contains fields
     * not present in the target DTO.
     *
     * @param ex      the message not readable exception
     * @param request the current HTTP request
     * @return 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpServletRequest request) {
        String message = ex.getMessage();
        if (message != null && message.contains("Unrecognized field")) {
            // Extract field name from error message for better UX
            message = "Invalid request: " + message.substring(0, Math.min(message.length(), 200));
        } else {
            message = "Malformed request body";
        }
        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        message,
                        "MALFORMED_REQUEST",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles unsupported HTTP methods (405).
     *
     * <p>Triggered when a request uses an HTTP method not supported by the endpoint.
     *
     * @param ex      the method not supported exception
     * @param request the current HTTP request
     * @return 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            final HttpRequestMethodNotSupportedException ex,
            final HttpServletRequest request) {
        String message = String.format("Method %s not supported for this endpoint", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        message,
                        "METHOD_NOT_ALLOWED",
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles domain-level validation failures (400).
     *
     * <p>Triggered by service-layer validation that throws IllegalArgumentException,
     * such as invalid business rule violations.
     *
     * @param ex      the illegal argument exception
     * @param request the current HTTP request
     * @return 400 Bad Request (or 404 if message indicates resource not found)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            final IllegalArgumentException ex,
            final HttpServletRequest request) {
        String message = ex.getMessage();
        
        // Check if this is a "not found" case
        if (message != null && message.toLowerCase().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponse(
                            Instant.now(),
                            HttpStatus.NOT_FOUND.value(),
                            message,
                            "NOT_FOUND",
                            request.getRequestURI()
                    )
            );
        }
        
        // Otherwise treat as bad request
        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        message != null ? message : "Invalid request",
                        "INVALID_REQUEST",
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

    // ---- private helpers -------------------------------------------------

    /**
     * Extracts the authenticated user's UUID from the JWT subject claim, or returns
     * {@code null} if no JWT is present in the security context.
     *
     * @return user UUID or null for unauthenticated contexts
     */
    private UUID extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String subject = jwt.getSubject();
                if (subject != null) {
                    return UUID.fromString(subject);
                }
            }
        } catch (Exception ignored) {
            // If we cannot extract the user ID, record null — audit log must never throw
        }
        return null;
    }

    /**
     * Persists an {@code ACCESS_DENIED} audit log entry.
     *
     * <p>Any exception from the audit service is swallowed — audit failures must
     * never prevent the 403 response from reaching the caller.
     *
     * @param userId       authenticated user UUID (may be null)
     * @param requestedUri the URI that triggered the access denial
     */
    private void recordAccessDeniedAuditLog(final UUID userId, final String requestedUri) {
        try {
            AuditLog entry = new AuditLog.Builder(ACCESS_DENIED_EVENT)
                    .userId(userId)
                    .detail(requestedUri)
                    .outcome("FAILURE")
                    .build();
            auditLogService.record(entry);
        } catch (Exception ex) {
            LOG.warn("Failed to record ACCESS_DENIED audit log for uri={}: {}",
                    requestedUri, ex.getMessage());
        }
    }
}
