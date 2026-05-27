package com.psyassistant.billing.pkg.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.billing.pkg.PackageDefinitionStatus;
import com.psyassistant.billing.pkg.PackageService;
import com.psyassistant.billing.pkg.dto.CreatePackageDefinitionRequest;
import com.psyassistant.billing.pkg.dto.PackageDefinitionResponse;
import com.psyassistant.billing.pkg.dto.UpdatePackageDefinitionStatusRequest;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
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
 * Web-layer tests for {@link PackageDefinitionController}.
 *
 * <p>Verifies HTTP status codes, JSON shape, security (RBAC), and error mappings.
 */
@WebMvcTest(controllers = PackageDefinitionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class PackageDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PackageService packageService;

    @MockitoBean
    private com.psyassistant.common.audit.AuditLogService auditLogService;

    private static final String BASE = "/api/v1/billing/package-definitions";
    private static final UUID DEF_ID = UUID.randomUUID();

    // =========================================================================
    // GET list
    // =========================================================================

    @Test
    void listReturns200ForReadPackagesAuthority() throws Exception {
        when(packageService.listDefinitions(null)).thenReturn(List.of(sampleDefinitionResponse()));

        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_PACKAGES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("5-Session Bundle"));
    }

    @Test
    void listFiltersWhenStatusParamProvided() throws Exception {
        when(packageService.listDefinitions(PackageDefinitionStatus.ACTIVE))
                .thenReturn(List.of(sampleDefinitionResponse()));

        mockMvc.perform(get(BASE).param("status", "ACTIVE")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_PACKAGES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void listReturns403WithoutReadPackagesAuthority() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("UNRELATED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listReturns401WithoutJwt() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // POST create
    // =========================================================================

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        CreatePackageDefinitionRequest request = new CreatePackageDefinitionRequest(
                "5-Session Bundle", ServiceType.INDIVIDUAL_SESSION, 5, new BigDecimal("250.00"));
        when(packageService.createDefinition(any(), any())).thenReturn(sampleDefinitionResponse());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_PACKAGES"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString(DEF_ID.toString())))
                .andExpect(jsonPath("$.name").value("5-Session Bundle"));
    }

    @Test
    void createReturns403WithoutManagePackagesAuthority() throws Exception {
        CreatePackageDefinitionRequest request = new CreatePackageDefinitionRequest(
                "Bundle", ServiceType.INDIVIDUAL_SESSION, 5, new BigDecimal("250.00"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_PACKAGES"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturns400WhenNameIsBlank() throws Exception {
        String body = "{\"name\":\"\",\"serviceType\":\"INDIVIDUAL_SESSION\",\"sessionQty\":5,\"price\":250.00}";

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_PACKAGES"))))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // PATCH status
    // =========================================================================

    @Test
    void updateStatusReturns200WithArchivedDefinition() throws Exception {
        PackageDefinitionResponse archived = new PackageDefinitionResponse(
                DEF_ID, "5-Session Bundle", ServiceType.INDIVIDUAL_SESSION,
                5, new BigDecimal("250.00"), new BigDecimal("50.00"),
                PackageDefinitionStatus.ARCHIVED, Instant.now(), Instant.now());

        when(packageService.updateDefinitionStatus(eq(DEF_ID), any(), any())).thenReturn(archived);

        UpdatePackageDefinitionStatusRequest request =
                new UpdatePackageDefinitionStatusRequest(PackageDefinitionStatus.ARCHIVED);

        mockMvc.perform(patch(BASE + "/" + DEF_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_PACKAGES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void updateStatusReturns403WithoutManagePackagesAuthority() throws Exception {
        UpdatePackageDefinitionStatusRequest request =
                new UpdatePackageDefinitionStatusRequest(PackageDefinitionStatus.ARCHIVED);

        mockMvc.perform(patch(BASE + "/" + DEF_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_PACKAGES"))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PackageDefinitionResponse sampleDefinitionResponse() {
        return new PackageDefinitionResponse(
                DEF_ID,
                "5-Session Bundle",
                ServiceType.INDIVIDUAL_SESSION,
                5,
                new BigDecimal("250.00"),
                new BigDecimal("50.00"),
                PackageDefinitionStatus.ACTIVE,
                Instant.now(),
                Instant.now());
    }
}
