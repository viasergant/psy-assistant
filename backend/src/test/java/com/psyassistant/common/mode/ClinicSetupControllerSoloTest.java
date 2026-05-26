package com.psyassistant.common.mode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that {@code POST /api/v1/setup/admin} returns HTTP 404 when the application
 * runs with the {@code solo} profile. The {@link ClinicSetupController} is annotated with
 * {@code @Profile("clinic")} and therefore must not be registered — and the path must
 * have no mapping — in solo mode.
 *
 * <p>Even though {@code @WebMvcTest(ClinicSetupController.class)} names the controller,
 * the {@code @Profile("clinic")} guard prevents Spring from registering it as a bean when
 * only the {@code solo} profile is active. The result is no handler mapping for the path,
 * so MockMvc returns HTTP 404.
 *
 * <p>The controller's dependencies are provided as {@code @MockitoBean}s so that the
 * application context can be assembled even though the controller itself is absent.
 */
@WebMvcTest(ClinicSetupController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles({"solo", "test"})
@TestPropertySource(properties = {
    "spring.mvc.throw-exception-if-no-handler-found=true",
    "spring.web.resources.add-mappings=false"
})
class ClinicSetupControllerSoloTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void whenSoloProfileSetupEndpointReturns404() throws Exception {
        // Arrange — clinic profile is not active; ClinicSetupController bean is not registered
        String body = "{\"email\":\"admin@clinic.com\",\"password\":\"SecureP@ss1\"}";

        // Act + Assert — no mapping exists for this path in solo mode
        mockMvc.perform(post("/api/v1/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
