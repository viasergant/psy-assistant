package com.psyassistant.auth.dto;

import java.time.Instant;

/**
 * Response body for a successful login or token refresh.
 *
 * <p>The refresh token is NOT included here — it is delivered via an HttpOnly cookie.
 *
 * @param accessToken          signed JWT access token
 * @param accessTokenExpiresAt when the access token expires (ISO-8601)
 * @param tokenType            always {@code "Bearer"}
 */
public record LoginResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String tokenType
) {
}
