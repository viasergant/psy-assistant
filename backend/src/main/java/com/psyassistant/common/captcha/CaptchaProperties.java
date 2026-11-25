package com.psyassistant.common.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CAPTCHA configuration (prefix {@code app.captcha}).
 *
 * @param enabled   whether public forms require a CAPTCHA token
 * @param siteKey   public site key exposed to the frontend widget
 * @param secretKey server-side secret used to verify tokens (never exposed)
 * @param verifyUrl provider verification endpoint
 */
@ConfigurationProperties(prefix = "app.captcha")
public record CaptchaProperties(boolean enabled, String siteKey, String secretKey, String verifyUrl) {

    /** Cloudflare Turnstile verification endpoint. */
    public static final String DEFAULT_VERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    /**
     * Applies defaults.
     *
     * @param enabled   whether CAPTCHA is required
     * @param siteKey   public site key
     * @param secretKey server secret
     * @param verifyUrl verification endpoint, defaults to Turnstile
     */
    public CaptchaProperties {
        if (verifyUrl == null || verifyUrl.isBlank()) {
            verifyUrl = DEFAULT_VERIFY_URL;
        }
    }
}
