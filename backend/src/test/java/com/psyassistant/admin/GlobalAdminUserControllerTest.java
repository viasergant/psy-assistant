package com.psyassistant.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.users.UserHasHistoryException;
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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor globalAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("GLOBAL_ADMIN"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
    }

    @Test
    void suspendReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post(BASE + "/" + id + "/suspension").with(globalAdmin()))
                .andExpect(status().isNoContent());
        verify(service).suspend(eq(id), any());
    }

    @Test
    void reactivateReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete(BASE + "/" + id + "/suspension").with(globalAdmin()))
                .andExpect(status().isNoContent());
        verify(service).reactivate(eq(id), any());
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete(BASE + "/" + id).with(globalAdmin()))
                .andExpect(status().isNoContent());
        verify(service).delete(eq(id), any());
    }

    @Test
    void deleteReturns409WhenUserHasHistory() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new UserHasHistoryException()).when(service).delete(eq(id), any());

        mockMvc.perform(delete(BASE + "/" + id).with(globalAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_HAS_HISTORY"));
    }

    @Test
    void mutationsReturn403ForSystemAdministratorAndTherapist() throws Exception {
        UUID id = UUID.randomUUID();
        var sysAdmin = jwt().authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMINISTRATOR"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
        mockMvc.perform(post(BASE + "/" + id + "/suspension").with(sysAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/" + id).with(sysAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutationsReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void statsReturns200ForGlobalAdmin() throws Exception {
        when(service.getStats()).thenReturn(new GlobalUserStatsDto(10, 3, 2));

        mockMvc.perform(get(BASE + "/stats").with(globalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.online").value(3))
                .andExpect(jsonPath("$.blocked").value(2));
    }

    @Test
    void statsReturns403ForSystemAdministratorAndTherapist() throws Exception {
        for (String authority : List.of("ROLE_SYSTEM_ADMINISTRATOR", "ROLE_THERAPIST")) {
            mockMvc.perform(get(BASE + "/stats")
                            .with(jwt().authorities(new SimpleGrantedAuthority(authority))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void statsReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/stats")).andExpect(status().isUnauthorized());
    }
}
