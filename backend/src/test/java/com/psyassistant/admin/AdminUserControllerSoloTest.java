package com.psyassistant.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.UserManagementService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that {@code GET /api/v1/admin/users} returns HTTP 404 when the application
 * runs with the {@code solo} profile. The {@link AdminUserController} is annotated with
 * {@code @Profile("clinic")} and therefore must not be registered — and the path must
 * have no mapping — in solo mode.
 *
 * <p>Even though {@code @WebMvcTest(AdminUserController.class)} names the controller,
 * the {@code @Profile("clinic")} guard prevents Spring from registering it as a bean when
 * only the {@code solo} profile is active. The result is no handler mapping for the path,
 * so MockMvc returns HTTP 404.
 *
 * <p>The request is made with a valid JWT so that Spring Security's auth filter passes and
 * the no-handler 404 is reached (without auth the filter would return 401 before any
 * handler lookup occurs).
 *
 * <p>The controller's dependencies are provided as {@code @MockitoBean}s so that the
 * application context can be assembled even though the controller itself is absent.
 */
@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles({"solo", "test"})
@TestPropertySource(properties = {
    "spring.mvc.throw-exception-if-no-handler-found=true",
    "spring.web.resources.add-mappings=false"
})
class AdminUserControllerSoloTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void whenSoloProfileAdminUsersEndpointReturns404() throws Exception {
        // Arrange — clinic profile is not active; AdminUserController bean is not registered
        // A valid JWT is provided so Spring Security passes auth; the missing handler yields 404
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMINISTRATOR"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound());
    }
}
