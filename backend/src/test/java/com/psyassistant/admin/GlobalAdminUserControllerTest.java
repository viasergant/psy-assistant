package com.psyassistant.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer tests for {@link GlobalAdminUserController}. */
@WebMvcTest(controllers = GlobalAdminUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles({"solo", "test"})
class GlobalAdminUserControllerTest {

    private static final String BASE = "/api/v1/global-admin/users";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GlobalAdminUserService service;

    @MockitoBean
    private AuditLogService auditLogService;

    private static GlobalUserPageResponse emptyPage() {
        return new GlobalUserPageResponse(List.of(), 0, 0, 0, 20);
    }

    @Test
    void returns200ForGlobalAdmin() throws Exception {
        when(service.listUsers(any(), any(), any(), any())).thenReturn(emptyPage());

        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("GLOBAL_ADMIN"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void returns403ForSystemAdministratorWithoutGlobalAdmin() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMINISTRATOR"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returns403ForTherapist() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_THERAPIST"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void clampsPageSizeAndFallsBackOnUnknownSortField() throws Exception {
        when(service.listUsers(any(), any(), any(), any())).thenReturn(emptyPage());

        mockMvc.perform(get(BASE).param("size", "1000").param("sort", "passwordHash,asc")
                        .with(jwt().authorities(new SimpleGrantedAuthority("GLOBAL_ADMIN"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listUsers(any(), any(), any(), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(100, captor.getValue().getPageSize());
        org.junit.jupiter.api.Assertions.assertNotNull(captor.getValue().getSort().getOrderFor("createdAt"));
    }

    @Test
    void passesFiltersToService() throws Exception {
        when(service.listUsers(any(), any(), any(), any())).thenReturn(emptyPage());

        mockMvc.perform(get(BASE).param("role", "THERAPIST").param("active", "true").param("search", "ann")
                        .with(jwt().authorities(new SimpleGrantedAuthority("GLOBAL_ADMIN"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        verify(service).listUsers(eq(UserRole.THERAPIST), eq(true), eq("ann"), any());
    }
}
