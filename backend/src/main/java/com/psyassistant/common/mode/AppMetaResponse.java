package com.psyassistant.common.mode;

/**
 * Response payload for the {@code GET /api/v1/meta} endpoint.
 *
 * @param mode    deployment mode — either {@code "SOLO"} or {@code "CLINIC"}
 * @param version application version from {@code spring.application.version}
 *                or {@code build.version}; falls back to {@code "unknown"}
 */
public record AppMetaResponse(String mode, String version) {
}
