package com.psyassistant.reporting.caseload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that {@code GET /api/v1/caseload} returns HTTP 200 when the application
 * runs with the {@code clinic} profile and the caller holds the
 * {@code READ_TEAM_WORKLOAD} authority.
 *
 * <p>{@link CaseloadController} is annotated with {@code @Profile("clinic")}, so it is
 * only registered — and its path is only mapped — when the {@code clinic} profile is active.
 * This test is the positive counterpart of {@link CaseloadControllerSoloTest}.
 */
@WebMvcTest(CaseloadController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles({"clinic", "test"})
class CaseloadControllerClinicTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseloadService caseloadService;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void givenClinicModeGetCaseloadReturns200() throws Exception {
        // Arrange — service returns an empty page; the controller only needs 200 and no exception
        when(caseloadService.getCaseload(
                any(), anyBoolean(), any(), any(), any()
        )).thenReturn(Page.empty());

        // Act + Assert
        mockMvc.perform(get("/api/v1/caseload")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("READ_TEAM_WORKLOAD"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
    }
}
