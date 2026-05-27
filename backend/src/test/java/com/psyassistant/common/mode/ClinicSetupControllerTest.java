package com.psyassistant.common.mode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link ClinicSetupController}.
 *
 * <p>Uses {@code @ActiveProfiles({"clinic", "test"})} so that the
 * {@link org.springframework.context.annotation.Profile @Profile("clinic")} guard on the
 * controller is satisfied and the bean is registered in the application context.
 *
 * <p>All collaborators are mocked — no database or real password encoder is involved.
 */
@WebMvcTest(ClinicSetupController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles({"clinic", "test"})
class ClinicSetupControllerTest {

    private static final String URL = "/api/v1/setup/admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuditLogService auditLogService;

    // ---- noUsersExist_createsAdmin_returns201 --------------------------------

    @Test
    void whenNoUsersExistCreatesAdminReturns201() throws Exception {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("SecureP@ss1")).thenReturn("$2a$12$hashed");

        UUID newId = UUID.randomUUID();
        User savedUser = new User("admin@clinic.com", "$2a$12$hashed", "Clinic Admin",
                Set.of(UserRole.SYSTEM_ADMINISTRATOR), true);
        ReflectionTestUtils.setField(savedUser, "id", newId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("admin@clinic.com", "SecureP@ss1", "Clinic Admin"));

        // Act + Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/setup/admin/" + newId))
                .andExpect(jsonPath("$.id").value(newId.toString()));

        verify(userRepository).save(any(User.class));
        verify(auditLogService).record(any(AuditLog.class));
    }

    // ---- usersAlreadyExist_returns409WithErrorResponse ----------------------

    @Test
    void whenUsersAlreadyExistReturns409WithErrorResponse() throws Exception {
        // Arrange
        when(userRepository.count()).thenReturn(1L);

        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("admin@clinic.com", "SecureP@ss1", null));

        // Act + Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SETUP_ALREADY_COMPLETE"))
                .andExpect(jsonPath("$.status").value(409));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---- invalidEmail_returns400WithErrorResponse ---------------------------

    @Test
    void whenInvalidEmailReturns400WithErrorResponse() throws Exception {
        // Arrange — email is not a valid address
        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("not-an-email", "SecureP@ss1", null));

        // Act + Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---- noAuth_requestAllowed ----------------------------------------------

    @Test
    void whenNoAuthRequestAllowed() throws Exception {
        // Arrange — no Authorization header; SecurityConfig must permitAll() for this path
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");

        UUID newId = UUID.randomUUID();
        User savedUser = new User("admin@clinic.com", "$2a$12$hashed",
                Set.of(UserRole.SYSTEM_ADMINISTRATOR), true);
        ReflectionTestUtils.setField(savedUser, "id", newId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("admin@clinic.com", "SecureP@ss1", null));

        // Act + Assert — no jwt() post-processor; should still return 201
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    // ---- auditLogFailure_returns201 (P3 swallow pattern) --------------------

    @Test
    void whenAuditLogThrowsCreatesAdminReturns201() throws Exception {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("SecureP@ss1")).thenReturn("$2a$12$hashed");

        UUID newId = UUID.randomUUID();
        User savedUser = new User("admin@clinic.com", "$2a$12$hashed", "Clinic Admin",
                Set.of(UserRole.SYSTEM_ADMINISTRATOR), true);
        ReflectionTestUtils.setField(savedUser, "id", newId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        doThrow(new RuntimeException("audit DB down")).when(auditLogService).record(any());

        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("admin@clinic.com", "SecureP@ss1", "Clinic Admin"));

        // Act + Assert — audit failure must not propagate; response must still be 201
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(newId.toString()));

        verify(userRepository).save(any(User.class));
    }

    // ---- passwordTooShort_returns400 ----------------------------------------

    @Test
    void whenPasswordTooShortReturns400() throws Exception {
        // Arrange — "short" is 5 chars, below @Size(min=8)
        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("admin@clinic.com", "short", null));

        // Act + Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---- blankEmail_returns400 ----------------------------------------------

    @Test
    void whenBlankEmailReturns400() throws Exception {
        // Arrange — blank email violates @NotBlank
        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("", "SecureP@ss1", null));

        // Act + Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---- blankPassword_returns400 -------------------------------------------

    @Test
    void whenBlankPasswordReturns400() throws Exception {
        // Arrange — blank password violates @NotBlank
        String body = objectMapper.writeValueAsString(
                new ClinicSetupRequest("admin@clinic.com", "", null));

        // Act + Assert
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userRepository, never()).save(any(User.class));
    }
}
