package com.psyassistant.auth.controller;

import com.psyassistant.auth.dto.LoginRequest;
import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.auth.service.AuthResult;
import com.psyassistant.auth.service.AuthService;
import com.psyassistant.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Provides login, token refresh, and logout operations.
 * The refresh token is delivered as an HttpOnly cookie; the access token is in the JSON body.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT-based login, refresh, and logout")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    /**
     * Constructs the controller with its required service.
     *
     * @param authService the authentication service
     */
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns a JWT access token plus a refresh token cookie.
     *
     * @param request  login credentials
     * @param response HTTP response (used to set cookie)
     * @param http     servlet request (used to extract caller IP)
     * @return 200 with {@link LoginResponse} or 401 on invalid credentials
     */
    @Operation(summary = "Login", description = "Authenticate with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful login",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials or disabled account",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody final LoginRequest request,
            final HttpServletResponse response,
            final HttpServletRequest http) {

        AuthResult result = authService.authenticate(request, getClientIp(http));
        setRefreshCookie(response, result);
        return ResponseEntity.ok(result.loginResponse());
    }

    /**
     * Rotates the refresh token and returns a new access token.
     *
     * @param http     servlet request containing the refresh token cookie
     * @param response HTTP response (used to set new cookie)
     * @return 200 with new {@link LoginResponse} or 401 if token is missing/expired
     */
    @Operation(summary = "Refresh tokens",
            description = "Exchange a valid refresh token cookie for new tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens refreshed",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token expired or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            final HttpServletRequest http,
            final HttpServletResponse response) {

        String rawToken = extractCookie(http, REFRESH_TOKEN_COOKIE);
        AuthResult result = authService.refresh(rawToken, getClientIp(http));
        setRefreshCookie(response, result);
        return ResponseEntity.ok(result.loginResponse());
    }

    /**
     * Revokes the user's refresh token and clears the cookie.
     *
     * @param http     servlet request containing the refresh token cookie
     * @param response HTTP response (cookie is cleared)
     * @return 204 No Content
     */
    @Operation(summary = "Logout", description = "Revoke refresh token and clear the cookie")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            final HttpServletRequest http,
            final HttpServletResponse response) {

        String rawToken = extractCookie(http, REFRESH_TOKEN_COOKIE);
        authService.logout(rawToken, getClientIp(http));
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    // ---- helpers -------------------------------------------------------

    private void setRefreshCookie(final HttpServletResponse response, final AuthResult result) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.rawRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(result.refreshTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(final HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractCookie(final HttpServletRequest request, final String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getClientIp(final HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
