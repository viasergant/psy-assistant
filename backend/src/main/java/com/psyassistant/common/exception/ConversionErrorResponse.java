package com.psyassistant.common.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Structured error payload returned for lead-already-converted (409) errors.
 *
 * <p>Extends the standard {@link ErrorResponse} shape with an {@code existingClientId}
 * field so callers can navigate directly to the existing client record.
 *
 * @param timestamp        when the error occurred
 * @param status           HTTP status code
 * @param error            short human-readable description
 * @param code             machine-readable error code (e.g. LEAD_ALREADY_CONVERTED)
 * @param path             request URI that triggered the error
 * @param existingClientId UUID of the existing client record, or null if unavailable
 */
public record ConversionErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String path,
        UUID existingClientId
) {
}
