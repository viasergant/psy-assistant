package com.psyassistant.users.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.dto.UpdateLanguageRequest;
import java.util.Optional;
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
}
