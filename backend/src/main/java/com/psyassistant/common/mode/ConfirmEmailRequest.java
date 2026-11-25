package com.psyassistant.common.mode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for confirming a registration email address.
 *
 * @param token raw token from the confirmation link
 */
public record ConfirmEmailRequest(
        @NotBlank
        @Size(max = 128)
        String token
) {
}
