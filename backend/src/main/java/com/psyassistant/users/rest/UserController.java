package com.psyassistant.users.rest;

import com.psyassistant.auth.dto.ChangePasswordRequest;
import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.auth.service.AuthResult;
import com.psyassistant.auth.service.AuthService;
import com.psyassistant.common.exception.ErrorResponse;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.dto.UpdateLanguageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for current user preferences and settings.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User Preferences", description = "Current user settings and preferences")
public class UserController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Constructs the controller with its required dependencies.
     *
     * @param userRepository user repository
     * @param authService    authentication service (password change)
     */
    public UserController(final UserRepository userRepository, final AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Updates the current user's language preference.
     *
     * @param request   language update request
     * @param principalName current user's ID as string (from JWT subject)
     * @return 204 No Content on success
     */
    @Operation(
            summary = "Update language preference",
            description = "Updates the current authenticated user's preferred language (en, uk)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Language updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid language code",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/language")
    public ResponseEntity<Void> updateLanguage(
            @Valid @RequestBody final UpdateLanguageRequest request,
            @AuthenticationPrincipal(expression = "subject") final String principalName) {

        UUID userId = UUID.fromString(principalName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLanguage(request.language());
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the current user's password.
     *
     * @param request       current and new password
     * @param principalName current user's ID as string (from JWT subject)
     * @param response      used to set the rotated refresh-token cookie
     * @param http          used to determine the caller's IP
     * @return 200 with a fresh access token
     */
    @Operation(
            summary = "Change own password",
            description = "Verifies the current password, applies the new one, revokes all other "
                    + "sessions and returns a fresh token pair for the calling session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400",
                description = "Validation error, wrong current password, same password or policy violation",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "423", description = "Account temporarily locked",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/password")
    public ResponseEntity<LoginResponse> changePassword(
            @Valid @RequestBody final ChangePasswordRequest request,
            @AuthenticationPrincipal(expression = "subject") final String principalName,
            final HttpServletResponse response,
            final HttpServletRequest http) {

        AuthResult result = authService.changePassword(
                UUID.fromString(principalName), request, clientIp(http));
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.rawRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(result.refreshTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(result.loginResponse());
    }

    private String clientIp(final HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
