package com.psyassistant.common.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Accepts every request. Active only when {@code app.captcha.enabled} is false (default),
 * e.g. in local development and automated tests.
 */
@Component
@ConditionalOnProperty(prefix = "app.captcha", name = "enabled", havingValue = "false",
        matchIfMissing = true)
public class DisabledCaptchaVerifier implements CaptchaVerifier {

    @Override
    public boolean verify(final String token, final String remoteIp) {
        return true;
    }
}
