package com.psyassistant.common.mode;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response payload for the {@code GET /api/v1/meta} endpoint.
 *
 * @param mode           deployment mode (SOLO, CLINIC or SAAS)
 * @param version        application version
 * @param captchaSiteKey public CAPTCHA site key; present only when CAPTCHA is enabled
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppMetaResponse(String mode, String version, String captchaSiteKey) {

    /**
     * Creates a response without a CAPTCHA site key.
     *
     * @param mode    deployment mode
     * @param version application version
     */
    public AppMetaResponse(final String mode, final String version) {
        this(mode, version, null);
    }
}
