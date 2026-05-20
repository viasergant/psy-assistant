package com.psyassistant.riskflags.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import com.psyassistant.riskflags.dto.CreateRiskFlagRequest;
import com.psyassistant.riskflags.dto.ResolveRiskFlagRequest;
import com.psyassistant.riskflags.dto.RiskFlagResponse;
import com.psyassistant.riskflags.service.RiskFlagService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link RiskFlagController}.
 *
 * <p>Key concern: {@code actorName} passed to the service must be a human-readable display name
 * resolved from {@link UserRepository}, NOT the raw JWT subject (UUID string).
 */
@WebMvcTest(controllers = RiskFlagController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class RiskFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RiskFlagService riskFlagService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private com.psyassistant.common.audit.AuditLogService auditLogService;

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID FLAG_ID    = UUID.randomUUID();
    private static final UUID ACTOR_ID   = UUID.randomUUID();
    private static final String BASE     = "/api/v1/clients/" + CLIENT_ID + "/risk-flags";

    // =========================================================================
    // POST / — create: actorName resolution
    // =========================================================================

    @Test
    void createFlagPassesFullNameAsActorNameWhenUserHasFullName() throws Exception {
        // Arrange
        when(userRepository.findById(ACTOR_ID))
                .thenReturn(Optional.of(userWithFullName("Jane Smith", "jane@example.com")));
        when(riskFlagService.createFlag(any(), any(), any(), any()))
                .thenReturn(sampleResponse(CLIENT_ID, FLAG_ID));

        // Act
        mockMvc.perform(post(BASE)
                        .with(jwt().jwt(b -> b.subject(ACTOR_ID.toString()))
                                   .authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        // Assert
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(riskFlagService).createFlag(eq(CLIENT_ID), any(), eq(ACTOR_ID), nameCaptor.capture());
        assert "Jane Smith".equals(nameCaptor.getValue())
                : "Expected 'Jane Smith' but got: " + nameCaptor.getValue();
    }

    @Test
    void createFlagPassesEmailAsActorNameWhenUserFullNameIsNull() throws Exception {
        // Arrange
        when(userRepository.findById(ACTOR_ID))
                .thenReturn(Optional.of(userWithFullName(null, "jane@example.com")));
        when(riskFlagService.createFlag(any(), any(), any(), any()))
                .thenReturn(sampleResponse(CLIENT_ID, FLAG_ID));

        // Act
        mockMvc.perform(post(BASE)
                        .with(jwt().jwt(b -> b.subject(ACTOR_ID.toString()))
                                   .authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        // Assert
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(riskFlagService).createFlag(eq(CLIENT_ID), any(), eq(ACTOR_ID), nameCaptor.capture());
        assert "jane@example.com".equals(nameCaptor.getValue())
                : "Expected 'jane@example.com' but got: " + nameCaptor.getValue();
    }

    @Test
    void createFlagFallsBackToUuidSubjectWhenUserNotFound() throws Exception {
        // Arrange
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.empty());
        when(riskFlagService.createFlag(any(), any(), any(), any()))
                .thenReturn(sampleResponse(CLIENT_ID, FLAG_ID));

        // Act
        mockMvc.perform(post(BASE)
                        .with(jwt().jwt(b -> b.subject(ACTOR_ID.toString()))
                                   .authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        // Assert
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(riskFlagService).createFlag(eq(CLIENT_ID), any(), eq(ACTOR_ID), nameCaptor.capture());
        assert ACTOR_ID.toString().equals(nameCaptor.getValue())
                : "Expected UUID fallback but got: " + nameCaptor.getValue();
    }

    @Test
    void createFlagReturns201WithLocationHeaderWhenValid() throws Exception {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(riskFlagService.createFlag(any(), any(), any(), any()))
                .thenReturn(sampleResponse(CLIENT_ID, FLAG_ID));

        // Act & Assert
        mockMvc.perform(post(BASE)
                        .with(jwt().jwt(b -> b.subject(ACTOR_ID.toString()))
                                   .authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/clients/" + CLIENT_ID + "/risk-flags/" + FLAG_ID));
    }

    @Test
    void createFlagReturns403WhenMissingManageRiskFlagsAuthority() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFlagReturns401WhenUnauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // PATCH /{flagId}/resolve
    // =========================================================================

    @Test
    void resolveFlagPassesFullNameAsActorName() throws Exception {
        // Arrange
        when(userRepository.findById(ACTOR_ID))
                .thenReturn(Optional.of(userWithFullName("Dr. Smith", "dr.smith@clinic.com")));
        when(riskFlagService.resolveFlag(any(), any(), any(), any(), any()))
                .thenReturn(sampleResponse(CLIENT_ID, FLAG_ID));

        // Act
        mockMvc.perform(patch(BASE + "/" + FLAG_ID + "/resolve")
                        .with(jwt().jwt(b -> b.subject(ACTOR_ID.toString()))
                                   .authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveRiskFlagRequest("Risk mitigated"))))
                .andExpect(status().isOk());

        // Assert
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(riskFlagService).resolveFlag(
                eq(CLIENT_ID), eq(FLAG_ID), any(), eq(ACTOR_ID), nameCaptor.capture());
        assert "Dr. Smith".equals(nameCaptor.getValue())
                : "Expected 'Dr. Smith' but got: " + nameCaptor.getValue();
    }

    @Test
    void resolveFlagReturns403WhenMissingManageRiskFlagsAuthority() throws Exception {
        // Act & Assert
        mockMvc.perform(patch(BASE + "/" + FLAG_ID + "/resolve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveRiskFlagRequest("note"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolveFlagReturns400WhenResolutionNoteIsBlank() throws Exception {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(patch(BASE + "/" + FLAG_ID + "/resolve")
                        .with(jwt().jwt(b -> b.subject(ACTOR_ID.toString()))
                                   .authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveRiskFlagRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET / and GET /history
    // =========================================================================

    @Test
    void listActiveFlagsReturns200WhenHasReadRiskFlagsAuthority() throws Exception {
        // Arrange
        when(riskFlagService.listActiveFlags(CLIENT_ID))
                .thenReturn(List.of(sampleResponse(CLIENT_ID, FLAG_ID)));

        // Act & Assert
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_RISK_FLAGS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(FLAG_ID.toString()));
    }

    @Test
    void listActiveFlagsReturns403WhenMissingReadRiskFlagsAuthority() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_RISK_FLAGS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listHistoryReturns200WhenHasReadRiskFlagNotesAuthority() throws Exception {
        // Arrange
        when(riskFlagService.listAllFlags(CLIENT_ID))
                .thenReturn(List.of(sampleResponse(CLIENT_ID, FLAG_ID)));

        // Act & Assert
        mockMvc.perform(get(BASE + "/history")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_RISK_FLAG_NOTES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(FLAG_ID.toString()));
    }

    @Test
    void listHistoryReturns403WhenMissingReadRiskFlagNotesAuthority() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE + "/history")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_RISK_FLAGS"))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User userWithFullName(final String fullName, final String email) {
        return new User(email, "hash", fullName, UserRole.THERAPIST, true);
    }

    private CreateRiskFlagRequest createRequest() {
        return new CreateRiskFlagRequest(UUID.randomUUID(), null, LocalDate.now().plusDays(1));
    }

    private RiskFlagResponse sampleResponse(final UUID clientId, final UUID flagId) {
        return new RiskFlagResponse(
                flagId,
                clientId,
                UUID.randomUUID(),
                "Self-Harm Risk",
                ClientRiskFlagStatus.ACTIVE,
                null,
                LocalDate.now().plusDays(7),
                ACTOR_ID,
                Instant.now(),
                null,
                null,
                null
        );
    }
}
