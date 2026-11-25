package com.psyassistant.common.mode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for re-sending the registration confirmation email.
 *
 * @param email        address used at registration
 * @param captchaToken CAPTCHA widget response (required when CAPTCHA is enabled)
 */
public record ResendConfirmationRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Size(max = 4096)
        String captchaToken
) {
}
