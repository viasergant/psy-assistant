package com.psyassistant.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.DuplicateEmailException;
import com.psyassistant.users.SelfDeactivationException;
import com.psyassistant.users.UserManagementService;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.dto.CreateUserRequest;
import com.psyassistant.users.dto.PatchUserRequest;
import com.psyassistant.users.dto.UserPageResponse;
import com.psyassistant.users.dto.UserSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link AdminUserController}.
 *
 * <p>Verifies HTTP status codes, JSON shape, security enforcement, and error mappings.
 */
@WebMvcTest(controllers = AdminUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementService userManagementService;

    private static final String BASE = "/api/v1/admin/users";
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    // ---- listUsers -------------------------------------------------------

    @Test
    void listUsersReturns200ForAdmin() throws Exception {
        UserPageResponse page = new UserPageResponse(List.of(), 0, 0, 0, 20);
        when(userManagementService.listUsers(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listUsersReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsersReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    // ---- createUser -------------------------------------------------------

    @Test
    void createUserReturns201OnSuccess() throws Exception {
        UserSummaryDto dto = new UserSummaryDto(
                USER_ID, "new@example.com", "Alice", UserRole.USER, true,
                Instant.now(), Instant.now());
        when(userManagementService.createUser(any(), any())).thenReturn(dto);

        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("new@example.com", "Alice", UserRole.USER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createUserReturns409OnDuplicateEmail() throws Exception {
        when(userManagementService.createUser(any(), any()))
                .thenThrow(new DuplicateEmailException("dup@example.com"));

        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("dup@example.com", "Bob", UserRole.USER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void createUserReturns400OnMissingEmail() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alice\",\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserReturns400OnInvalidEmail() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("not-an-email", "Alice", UserRole.USER))))
                .andExpect(status().isBadRequest());
    }

    // ---- updateUser -------------------------------------------------------

    @Test
    void updateUserReturns200OnSuccess() throws Exception {
        UserSummaryDto dto = new UserSummaryDto(
                USER_ID, "user@example.com", "Updated", UserRole.ADMIN, true,
                Instant.now(), Instant.now());
        when(userManagementService.updateUser(eq(USER_ID), any(), any())).thenReturn(dto);

        mockMvc.perform(patch(BASE + "/" + USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatchUserRequest(null, UserRole.ADMIN, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateUserReturns400OnSelfDeactivation() throws Exception {
        when(userManagementService.updateUser(any(), any(), any()))
                .thenThrow(new SelfDeactivationException());

        mockMvc.perform(patch(BASE + "/" + ADMIN_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatchUserRequest(null, null, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_DEACTIVATION_FORBIDDEN"));
    }

    @Test
    void updateUserReturns404WhenUserNotFound() throws Exception {
        when(userManagementService.updateUser(eq(USER_ID), any(), any()))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(patch(BASE + "/" + USER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PatchUserRequest(null, null, false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ---- initiatePasswordReset -------------------------------------------

    @Test
    void passwordResetReturns204OnSuccess() throws Exception {
        when(userManagementService.initiatePasswordReset(eq(USER_ID), any()))
                .thenReturn("deadbeef");

        mockMvc.perform(post(BASE + "/" + USER_ID + "/password-reset")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void passwordResetReturns404WhenUserNotFound() throws Exception {
        when(userManagementService.initiatePasswordReset(eq(USER_ID), any()))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(post(BASE + "/" + USER_ID + "/password-reset")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject(ADMIN_ID.toString()))))
                .andExpect(status().isNotFound());
    }
}
