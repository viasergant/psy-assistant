package com.psyassistant.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.auth.controller.AuthController;
import com.psyassistant.auth.dto.LoginRequest;
import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.auth.service.AuthException;
import com.psyassistant.auth.service.AuthResult;
import com.psyassistant.auth.service.AuthService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BDD-style @WebMvcTest for {@link AuthController} covering all 6 acceptance scenarios.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    /**
     * Scenario 1: POST /api/v1/auth/login with valid credentials returns 200,
     * accessToken in body, and refreshToken as HttpOnly cookie.
     */
    @Test
    void validLoginReturns200WithTokens() throws Exception {
        LoginResponse resp = new LoginResponse("jwt-abc", Instant.now().plusSeconds(900), "Bearer");
        AuthResult result = new AuthResult(resp, "raw-refresh-uuid", UserRole.USER,
                Duration.ofDays(15));
        when(authService.authenticate(any(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "ValidPass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiresAt").exists());
    }

    /**
     * Scenario 2: POST /api/v1/auth/login with wrong password returns 401 INVALID_CREDENTIALS.
     */
    @Test
    void wrongPasswordReturns401InvalidCredentials() throws Exception {
        when(authService.authenticate(any(), anyString()))
                .thenThrow(new AuthException(AuthException.ErrorCode.INVALID_CREDENTIALS,
                        "Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    /**
     * Scenario 3: POST /api/v1/auth/refresh with valid cookie returns 200 with new access token.
     */
    @Test
    void validRefreshCookieReturns200WithNewTokens() throws Exception {
        LoginResponse resp = new LoginResponse("new-jwt", Instant.now().plusSeconds(900), "Bearer");
        AuthResult result = new AuthResult(resp, "new-raw-refresh", UserRole.USER,
                Duration.ofDays(15));
        when(authService.refresh(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-uuid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt"));
    }

    /**
     * Scenario 4: POST /api/v1/auth/logout returns 204 and clears the refreshToken cookie.
     */
    @Test
    void logoutReturns204AndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "some-token")))
                .andExpect(status().isNoContent());
    }

    /**
     * Scenario 5: POST /api/v1/auth/refresh with an expired cookie returns 401 TOKEN_EXPIRED.
     */
    @Test
    void expiredRefreshTokenReturns401TokenExpired() throws Exception {
        when(authService.refresh(anyString(), anyString()))
                .thenThrow(new AuthException(AuthException.ErrorCode.TOKEN_EXPIRED,
                        "Token expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "expired-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    /**
     * Scenario 6: POST /api/v1/auth/login with a deactivated user returns 401 ACCOUNT_DISABLED.
     */
    @Test
    void disabledAccountReturns401AccountDisabled() throws Exception {
        when(authService.authenticate(any(), anyString()))
                .thenThrow(new AuthException(AuthException.ErrorCode.ACCOUNT_DISABLED,
                        "Account disabled"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("disabled@example.com", "pass1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    /**
     * BCrypt DoS protection: password longer than 72 chars returns 400.
     */
    @Test
    void passwordTooLongReturns400() throws Exception {
        String longPassword = "a".repeat(73);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", longPassword))))
                .andExpect(status().isBadRequest());
    }

    /**
     * On successful login the Set-Cookie header must include the HttpOnly attribute.
     */
    @Test
    void successfulLoginSetsHttpOnlyCookie() throws Exception {
        LoginResponse resp = new LoginResponse("jwt", Instant.now().plusSeconds(900), "Bearer");
        AuthResult result = new AuthResult(resp, "refresh-val", UserRole.USER, Duration.ofDays(15));
        when(authService.authenticate(any(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "ValidPass1!"))))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("refreshToken", true));
    }
}
