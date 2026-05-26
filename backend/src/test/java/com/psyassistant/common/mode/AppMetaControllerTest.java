package com.psyassistant.common.mode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code @WebMvcTest} for {@link AppMetaController} covering mode detection and unauthenticated access.
 */
@WebMvcTest(controllers = AppMetaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AppMetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * When neither {@code solo} nor {@code clinic} profile is active (only {@code test}),
     * GET /api/v1/meta defaults to mode="SOLO" and returns HTTP 200.
     */
    @Test
    void defaultModeReturnsSoloWhenNoModeProfileActive() throws Exception {
        // Arrange — only "test" profile is active; controller defaults to SOLO
        // Act + Assert
        mockMvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SOLO"))
                .andExpect(jsonPath("$.version").exists());
    }

    /**
     * GET /api/v1/meta must return HTTP 200 without an Authorization header
     * (verifies the SecurityConfig permitAll rule is in effect).
     */
    @Test
    void noAuthReturnsOk() throws Exception {
        // Arrange — no Authorization header added
        // Act + Assert
        mockMvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk());
    }
}
