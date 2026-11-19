package com.psyassistant.common.mode;

import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.auth.service.AuthResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for self-service solo therapist registration.
 *
 * <p>Active only in the {@code saas} Spring profile. Creates a new
 * {@link com.psyassistant.organization.Organization} and owner
 * {@link com.psyassistant.users.User}, then immediately issues JWT tokens so the
 * caller is authenticated without a separate login step.
 *
 * <p>The refresh token is delivered as an HttpOnly cookie on the
 * {@code /api/v1/auth} path so it is automatically included in subsequent token
 * refresh requests.
 */
@RestController
@Profile("saas")
@RequestMapping("/api/v1/register")
public class RegistrationController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final RegistrationService registrationService;

    public RegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Registers a new solo therapist organization.
     *
     * @param request  validated registration data
     * @param response HTTP response used to set the refresh-token cookie
     * @return 201 Created with {@link LoginResponse} body and {@code Location} header
     */
    @PostMapping
    public ResponseEntity<LoginResponse> register(
            @Valid @RequestBody final RegistrationRequest request,
            final HttpServletResponse response) {

        AuthResult result = registrationService.register(request);
        setRefreshCookie(response, result);

        return ResponseEntity
                .created(URI.create("/api/v1/meta"))
                .body(result.loginResponse());
    }

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
}
