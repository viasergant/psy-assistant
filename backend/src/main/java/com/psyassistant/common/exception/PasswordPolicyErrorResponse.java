package com.psyassistant.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Error response body returned when a password fails complexity validation.
 *
 * @param timestamp  when the error occurred
 * @param status     HTTP status code
 * @param error      human-readable description
 * @param code       machine-readable error code ({@code PASSWORD_POLICY_VIOLATION})
 * @param path       request URI that triggered the error
 * @param violations list of specific constraint violation codes
 */
public record PasswordPolicyErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String path,
        List<String> violations) {
}
