package com.psyassistant.auth.service;

import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.users.UserRole;
import java.time.Duration;
import java.util.Set;

/**
 * Internal value object returned by {@link AuthService} authentication methods.
 *
 * <p>Carries both the JSON response body and the raw refresh token that the
 * controller will set as an HttpOnly cookie.
 *
 * @param loginResponse    the JSON body to send to the client
 * @param rawRefreshToken  the plain UUID refresh token for the Set-Cookie header
 * @param roles            the authenticated user's roles (used to compute cookie max-age);
 *                         never null, never empty
 * @param refreshTtl       TTL of the issued refresh token
 */
public record AuthResult(
        LoginResponse loginResponse,
        String rawRefreshToken,
        Set<UserRole> roles,
        Duration refreshTtl
) {
}
