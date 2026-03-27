package com.psyassistant.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that {@link GlobalExceptionHandler} returns a structured JSON error body
 * with no stack trace when an unhandled exception is thrown by a controller.
 */
@WebMvcTest(controllers = TestThrowingController.class)
@Import({GlobalExceptionHandler.class})
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    /**
     * When a controller throws a RuntimeException the handler must return
     * HTTP 500 with timestamp, status, error, and path fields — no stack trace.
     */
    @Test
    @WithMockUser
    void whenUnhandledExceptionThenReturnsStructuredErrorResponse() throws Exception {
        mockMvc.perform(get("/test/throw"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/test/throw"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
