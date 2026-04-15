package com.psyassistant.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration for security-related policy settings.
 *
 * <p>Bound to the {@code app.security} YAML namespace.
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        LockoutProperties lockout,
        PasswordProperties password,
        PasswordResetProperties passwordReset
) {

    /**
     * Account lockout policy.
     *
     * @param maxAttempts     number of consecutive failures before lockout
     * @param durationMinutes how long (in minutes) the account stays locked
     */
    public record LockoutProperties(int maxAttempts, int durationMinutes) {
    }

    /**
     * Password complexity requirements.
     *
     * @param minLength          minimum number of characters
     * @param requireUppercase   at least one uppercase letter required
     * @param requireDigit       at least one digit required
     * @param requireSpecialChar at least one special character required
     */
    public record PasswordProperties(
            int minLength,
            boolean requireUppercase,
            boolean requireDigit,
            boolean requireSpecialChar) {
    }

    /**
     * Password-reset token settings.
     *
     * @param tokenTtlHours number of hours until a reset token expires
     */
    public record PasswordResetProperties(int tokenTtlHours) {
    }
}
