package com.psyassistant.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds the configured global-admin username (email).
 *
 * <p>When non-blank, the user whose email matches this value receives a
 * {@code GLOBAL_ADMIN} authority in their JWT, granting access to
 * system-wide settings (e.g. risk flag type configuration) that are
 * not tied to any single organisation.
 *
 * <p>Bound from {@code app.global-admin.username} in {@code application.yml}.
 * Set via the {@code GLOBAL_ADMIN_USERNAME} environment variable in production.
 */
@ConfigurationProperties(prefix = "app.global-admin")
public record GlobalAdminProperties(String username) {

    /** Returns true when a global-admin username has been configured. */
    public boolean isConfigured() {
        return username != null && !username.isBlank();
    }

    /** Returns true when {@code email} matches the configured global-admin username. */
    public boolean matches(final String email) {
        return isConfigured() && username.equalsIgnoreCase(email);
    }
}
