package com.psyassistant.common.captcha;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Cloudflare Turnstile implementation of {@link CaptchaVerifier}.
 *
 * <p>Fails closed: any provider error or missing token is treated as a failed check.
 */
@Component
@ConditionalOnProperty(prefix = "app.captcha", name = "enabled", havingValue = "true")
public class TurnstileCaptchaVerifier implements CaptchaVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(TurnstileCaptchaVerifier.class);

    private final CaptchaProperties properties;
    private final RestClient restClient;

    /**
     * Constructs the verifier.
     *
     * @param properties captcha configuration
     */
    public TurnstileCaptchaVerifier(final CaptchaProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean verify(final String token, final String remoteIp) {
        if (token == null || token.isBlank()) {
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.secretKey());
        form.add("response", token);
        if (remoteIp != null) {
            form.add("remoteip", remoteIp);
        }
        try {
            Map<?, ?> body = restClient.post()
                    .uri(properties.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return body != null && Boolean.TRUE.equals(body.get("success"));
        } catch (RuntimeException ex) {
            LOG.warn("event=CAPTCHA_VERIFY_ERROR reason={}", ex.getClass().getSimpleName());
            return false;
        }
    }
}
