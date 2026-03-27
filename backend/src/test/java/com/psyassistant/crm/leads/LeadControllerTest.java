package com.psyassistant.crm.leads;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.crm.leads.dto.ContactMethodDto;
import com.psyassistant.crm.leads.dto.ContactMethodRequest;
import com.psyassistant.crm.leads.dto.ConvertLeadRequest;
import com.psyassistant.crm.leads.dto.ConvertLeadResponse;
import com.psyassistant.crm.leads.dto.CreateLeadRequest;
import com.psyassistant.crm.leads.dto.LeadDetailDto;
import com.psyassistant.crm.leads.dto.LeadPageResponse;
import com.psyassistant.crm.leads.dto.TransitionStatusRequest;
import com.psyassistant.crm.leads.dto.UpdateLeadRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
 * Web-layer tests for {@link LeadController}.
 *
 * <p>Verifies HTTP status codes, JSON shape, security enforcement (RBAC), and error mappings.
 */
@WebMvcTest(controllers = LeadController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeadService leadService;

    @MockitoBean
    private AuditLogService auditLogService;

    private static final String BASE = "/api/v1/leads";
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID LEAD_ID = UUID.randomUUID();

    // ---- POST create lead -------------------------------------------------

    @Test
    void postLeadReturns201ForManageLeadsAuthority() throws Exception {
        LeadDetailDto dto = makeDetailDto(LEAD_ID, "Jane Smith", LeadStatus.NEW);
        when(leadService.createLead(any(), any())).thenReturn(dto);

        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLeadRequest(
                                "Jane Smith",
                                List.of(new ContactMethodRequest(
                                        "EMAIL", "jane@example.com", true)),
                                null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Jane Smith"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void postLeadReturns403ForTherapistRole() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_THERAPIST"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLeadRequest(
                                "Jane Smith",
                                List.of(new ContactMethodRequest(
                                        "EMAIL", "j@example.com", true)),
                                null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postLeadReturns403ForSupervisorReadLeadsAuthority() throws Exception {
        // READ_LEADS is for read-only — callers with only READ_LEADS cannot create
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLeadRequest(
                                "Jane Smith",
                                List.of(new ContactMethodRequest(
                                        "EMAIL", "j@example.com", true)),
                                null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postLeadReturns400WhenContactMethodsEmpty() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane\",\"contactMethods\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postLeadReturns400WhenFullNameMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactMethods\":"
                                + "[{\"type\":\"EMAIL\",\"value\":\"a@b.com\",\"isPrimary\":true}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postLeadReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET list leads ---------------------------------------------------

    @Test
    void getLeadsReturns200ForReadLeadsAuthority() throws Exception {
        LeadPageResponse page = new LeadPageResponse(List.of(), 0, 0, 0, 20);
        when(leadService.listLeads(any(), any(), eq(false), any())).thenReturn(page);

        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getLeadsReturns200ForManageLeadsAuthority() throws Exception {
        LeadPageResponse page = new LeadPageResponse(List.of(), 0, 0, 0, 20);
        when(leadService.listLeads(any(), any(), eq(false), any())).thenReturn(page);

        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void getLeadsReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET single lead --------------------------------------------------

    @Test
    void getLeadReturns200ForReadLeadsAuthority() throws Exception {
        LeadDetailDto dto = makeDetailDto(LEAD_ID, "Jane Smith", LeadStatus.NEW);
        when(leadService.getLead(LEAD_ID)).thenReturn(dto);

        mockMvc.perform(get(BASE + "/" + LEAD_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LEAD_ID.toString()));
    }

    @Test
    void getLeadReturns404WhenNotFound() throws Exception {
        when(leadService.getLead(LEAD_ID))
                .thenThrow(new EntityNotFoundException("Lead not found: " + LEAD_ID));

        mockMvc.perform(get(BASE + "/" + LEAD_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getLeadReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/" + LEAD_ID))
                .andExpect(status().isUnauthorized());
    }

    // ---- PUT update lead --------------------------------------------------

    @Test
    void putLeadReturns200ForManageLeadsAuthority() throws Exception {
        LeadDetailDto dto = makeDetailDto(LEAD_ID, "Updated Name", LeadStatus.NEW);
        when(leadService.updateLead(eq(LEAD_ID), any(), any())).thenReturn(dto);

        mockMvc.perform(put(BASE + "/" + LEAD_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateLeadRequest(
                                "Updated Name",
                                List.of(new ContactMethodRequest(
                                        "EMAIL", "upd@example.com", true)),
                                null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    void putLeadReturns403ForReadLeadsAuthority() throws Exception {
        mockMvc.perform(put(BASE + "/" + LEAD_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateLeadRequest(
                                "Name",
                                List.of(new ContactMethodRequest("EMAIL", "a@b.com", true)),
                                null, null, null))))
                .andExpect(status().isForbidden());
    }

    // ---- PATCH /status ----------------------------------------------------

    @Test
    void patchStatusReturns422OnInvalidTransition() throws Exception {
        when(leadService.transitionStatus(eq(LEAD_ID), eq(LeadStatus.NEW), any()))
                .thenThrow(new InvalidStatusTransitionException(
                        LeadStatus.CONTACTED, LeadStatus.NEW));

        mockMvc.perform(patch(BASE + "/" + LEAD_ID + "/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransitionStatusRequest(LeadStatus.NEW))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void patchStatusReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch(BASE + "/" + LEAD_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- PATCH /archive ---------------------------------------------------

    @Test
    void patchArchiveReturns422WhenConverted() throws Exception {
        when(leadService.archiveLead(eq(LEAD_ID), any()))
                .thenThrow(new InvalidStatusTransitionException(
                        LeadStatus.CONVERTED, LeadStatus.INACTIVE));

        mockMvc.perform(patch(BASE + "/" + LEAD_ID + "/archive")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void patchArchiveReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch(BASE + "/" + LEAD_ID + "/archive"))
                .andExpect(status().isUnauthorized());
    }

    // ---- POST /{id}/convert -----------------------------------------------

    @Test
    void postConvertReturns201ForManageLeadsAuthority() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(leadService.convertLead(eq(LEAD_ID), any(), any()))
                .thenReturn(new ConvertLeadResponse(clientId, LEAD_ID));

        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConvertLeadRequest(
                                "Jane Smith",
                                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                                null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.leadId").value(LEAD_ID.toString()));
    }

    @Test
    void postConvertReturns403ForReadLeadsAuthority() throws Exception {
        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConvertLeadRequest(
                                "Jane",
                                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                                null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postConvertReturns422WhenInvalidStatusTransition() throws Exception {
        when(leadService.convertLead(eq(LEAD_ID), any(), any()))
                .thenThrow(new InvalidStatusTransitionException(
                        LeadStatus.NEW, LeadStatus.CONVERTED));

        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConvertLeadRequest(
                                "Jane",
                                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                                null, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void postConvertReturns409WhenAlreadyConverted() throws Exception {
        UUID existingClientId = UUID.randomUUID();
        when(leadService.convertLead(eq(LEAD_ID), any(), any()))
                .thenThrow(new LeadAlreadyConvertedException(LEAD_ID, existingClientId));

        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConvertLeadRequest(
                                "Jane",
                                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                                null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEAD_ALREADY_CONVERTED"))
                .andExpect(jsonPath("$.existingClientId").value(existingClientId.toString()));
    }

    @Test
    void postConvertReturns404WhenLeadNotFound() throws Exception {
        when(leadService.convertLead(eq(LEAD_ID), any(), any()))
                .thenThrow(new EntityNotFoundException("Lead not found: " + LEAD_ID));

        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConvertLeadRequest(
                                "Jane",
                                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                                null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void postConvertReturns400WhenFullNameMissing() throws Exception {
        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactMethods\":"
                                + "[{\"type\":\"EMAIL\",\"value\":\"j@example.com\",\"isPrimary\":true}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postConvertReturns400WhenContactMethodsMissing() throws Exception {
        mockMvc.perform(post(BASE + "/" + LEAD_ID + "/convert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_LEADS"))
                                .jwt(j -> j.subject(ACTOR_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane\",\"contactMethods\":[]}"))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers ----------------------------------------------------------

    private LeadDetailDto makeDetailDto(final UUID id, final String fullName,
                                        final LeadStatus status) {
        return new LeadDetailDto(
                id,
                fullName,
                List.of(new ContactMethodDto(
                        UUID.randomUUID(), "EMAIL", "jane@example.com", true)),
                "referral",
                status,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                "actor@example.com",
                null
        );
    }
}
