package com.psyassistant.common.captcha;

/**
 * Port for verifying a CAPTCHA response token supplied by a public form.
 */
public interface CaptchaVerifier {

    /**
     * Verifies the token with the CAPTCHA provider.
     *
     * @param token    response token from the client widget (may be null)
     * @param remoteIp caller IP, forwarded to the provider when available (may be null)
     * @return true only if the provider confirmed the token
     */
    boolean verify(String token, String remoteIp);
}
