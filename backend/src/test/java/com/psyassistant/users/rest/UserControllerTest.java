package com.psyassistant.users.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.auth.service.AuthResult;
import com.psyassistant.auth.service.AuthService;
import com.psyassistant.auth.service.PasswordChangeException;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.dto.UpdateLanguageRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Unit tests for {@link UserController}.
 */
@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    @MockBean
    private LocaleResolver localeResolver;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void updateLanguageReturnsNoContentWhenSuccessful() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        User user = new User("test@example.com", "hash", UserRole.THERAPIST, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateLanguageRequest request = new UpdateLanguageRequest("uk");

        mockMvc.perform(patch("/api/v1/users/me/language")
                .with(jwt().jwt(j -> j.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userRepository).save(user);
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void updateLanguageReturnsBadRequestWhenLanguageInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/language")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\":\"fr\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void updateLanguageReturnsBadRequestWhenLanguageMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/language")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLanguageReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        UpdateLanguageRequest request = new UpdateLanguageRequest("uk");

        mockMvc.perform(patch("/api/v1/users/me/language")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private static final String PASSWORD_URL = "/api/v1/users/me/password";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void changePasswordReturnsTokenAndSetsRefreshCookie() throws Exception {
        LoginResponse body = new LoginResponse("new-jwt", Instant.now().plusSeconds(3600), "Bearer", "en");
        when(authService.changePassword(any(), any(), any())).thenReturn(
                new AuthResult(body, "raw-refresh", Set.of(UserRole.THERAPIST), Duration.ofDays(15)));

        mockMvc.perform(post(PASSWORD_URL)
                .with(csrf())
                .with(jwt().jwt(j -> j.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"OldPass1!\",\"newPassword\":\"NewPass1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("refreshToken=raw-refresh")));
    }

    @Test
    void changePasswordReturnsBadRequestWhenFieldsMissing() throws Exception {
        mockMvc.perform(post(PASSWORD_URL)
                .with(csrf())
                .with(jwt().jwt(j -> j.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordReturnsBadRequestWhenCurrentPasswordWrong() throws Exception {
        when(authService.changePassword(any(), any(), any())).thenThrow(new PasswordChangeException(
                PasswordChangeException.Reason.INVALID_CURRENT_PASSWORD, "Current password is incorrect"));

        mockMvc.perform(post(PASSWORD_URL)
                .with(csrf())
                .with(jwt().jwt(j -> j.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"NewPass1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void changePasswordReturnsLockedWhenAccountLocked() throws Exception {
        when(authService.changePassword(any(), any(), any())).thenThrow(new PasswordChangeException(
                PasswordChangeException.Reason.ACCOUNT_LOCKED, "locked"));

        mockMvc.perform(post(PASSWORD_URL)
                .with(csrf())
                .with(jwt().jwt(j -> j.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"NewPass1!\"}"))
                .andExpect(status().isLocked());
    }

    @Test
    void changePasswordReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post(PASSWORD_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"NewPass1!\"}"))
                .andExpect(status().isUnauthorized());
    }
}
