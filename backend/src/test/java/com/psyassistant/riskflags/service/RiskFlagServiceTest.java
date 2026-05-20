package com.psyassistant.riskflags.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.riskflags.domain.ClientRiskFlag;
import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import com.psyassistant.riskflags.domain.RiskFlagAuditLog;
import com.psyassistant.riskflags.domain.RiskFlagType;
import com.psyassistant.riskflags.dto.CreateRiskFlagRequest;
import com.psyassistant.riskflags.dto.ResolveRiskFlagRequest;
import com.psyassistant.riskflags.dto.RiskFlagResponse;
import com.psyassistant.riskflags.repository.ClientRiskFlagRepository;
import com.psyassistant.riskflags.repository.RiskFlagAuditLogRepository;
import com.psyassistant.riskflags.repository.RiskFlagTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link RiskFlagService}.
 *
 * <p>All tests use Mockito only — no Spring context, no infrastructure dependencies.
 */
@ExtendWith(MockitoExtension.class)
class RiskFlagServiceTest {

    @Mock
    private ClientRiskFlagRepository flagRepository;

    @Mock
    private RiskFlagTypeRepository flagTypeRepository;

    @Mock
    private RiskFlagAuditLogRepository auditLogRepository;

    @Mock
    private ClientRepository clientRepository;

    private final UUID clientId = UUID.randomUUID();
    private final UUID flagTypeId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private RiskFlagService buildService() {
        return new RiskFlagService(flagRepository, flagTypeRepository, auditLogRepository, clientRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // createFlag — happy path
    // =========================================================================

    @Test
    @DisplayName("createFlag - persists flag and appends audit when caller has MANAGE_RISK_FLAGS")
    void createFlagPersistsFlagAndAppendsAuditWhenCallerHasManageRiskFlags() {
        // Arrange
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_CLIENTS_ALL");

        final RiskFlagType flagType = activeFlagType("Self-Harm Risk");
        when(flagTypeRepository.findById(flagTypeId)).thenReturn(Optional.of(flagType));

        final ClientRiskFlag savedFlag = makeFlag(clientId, flagTypeId, actorId);
        when(flagRepository.save(any(ClientRiskFlag.class))).thenReturn(savedFlag);
        when(auditLogRepository.save(any(RiskFlagAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final CreateRiskFlagRequest request = new CreateRiskFlagRequest(
                flagTypeId, "Clinical observation", LocalDate.now().plusDays(7));

        // Act
        final RiskFlagResponse response = buildService()
                .createFlag(clientId, request, actorId, "Dr. Smith");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ClientRiskFlagStatus.ACTIVE);
        assertThat(response.flagTypeName()).isEqualTo("Self-Harm Risk");
        verify(flagRepository, times(1)).save(any(ClientRiskFlag.class));
        verify(auditLogRepository, times(1)).save(any(RiskFlagAuditLog.class));
    }

    // =========================================================================
    // createFlag — permission denied
    // =========================================================================

    @Test
    @DisplayName("createFlag - throws AccessDeniedException when caller lacks MANAGE_RISK_FLAGS")
    void createFlagThrowsAccessDeniedWhenCallerLacksManageRiskFlags() {
        // Arrange — only READ_RISK_FLAGS, no manage permission
        setAuth(actorId, "READ_RISK_FLAGS");

        final CreateRiskFlagRequest request = new CreateRiskFlagRequest(
                flagTypeId, null, LocalDate.now().plusDays(7));

        // Act & Assert
        assertThatThrownBy(() -> buildService().createFlag(clientId, request, actorId, "Someone"))
                .isInstanceOf(AccessDeniedException.class);

        verify(flagRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // =========================================================================
    // createFlag — THERAPIST not assigned
    // =========================================================================

    @Test
    @DisplayName("createFlag - throws AccessDeniedException when THERAPIST is not assigned to client")
    void createFlagThrowsAccessDeniedWhenTherapistNotAssignedToClient() {
        // Arrange — therapist role: has MANAGE_RISK_FLAGS and READ_ASSIGNED_CLIENTS but NOT READ_CLIENTS_ALL
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_ASSIGNED_CLIENTS");

        final UUID differentTherapistId = UUID.randomUUID();
        final Client client = makeClient(clientId, differentTherapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        final CreateRiskFlagRequest request = new CreateRiskFlagRequest(
                flagTypeId, null, LocalDate.now().plusDays(7));

        // Act & Assert
        assertThatThrownBy(() -> buildService().createFlag(clientId, request, actorId, "Dr. Jones"))
                .isInstanceOf(AccessDeniedException.class);

        verify(flagRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // =========================================================================
    // createFlag — THERAPIST assigned (succeeds)
    // =========================================================================

    @Test
    @DisplayName("createFlag - succeeds when THERAPIST is assigned to client")
    void createFlagSucceedsWhenTherapistIsAssignedToClient() {
        // Arrange — therapist role: MANAGE_RISK_FLAGS + READ_ASSIGNED_CLIENTS, NOT READ_CLIENTS_ALL
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_ASSIGNED_CLIENTS");

        final Client client = makeClient(clientId, actorId); // actor IS assigned therapist
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        final RiskFlagType flagType = activeFlagType("Crisis History");
        when(flagTypeRepository.findById(flagTypeId)).thenReturn(Optional.of(flagType));

        final ClientRiskFlag savedFlag = makeFlag(clientId, flagTypeId, actorId);
        when(flagRepository.save(any(ClientRiskFlag.class))).thenReturn(savedFlag);
        when(auditLogRepository.save(any(RiskFlagAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final CreateRiskFlagRequest request = new CreateRiskFlagRequest(
                flagTypeId, "Note", LocalDate.now().plusDays(14));

        // Act
        final RiskFlagResponse response = buildService()
                .createFlag(clientId, request, actorId, "Dr. Jones");

        // Assert
        assertThat(response).isNotNull();
        verify(flagRepository, times(1)).save(any(ClientRiskFlag.class));
        verify(auditLogRepository, times(1)).save(any(RiskFlagAuditLog.class));
    }

    // =========================================================================
    // createFlag — inactive flag type
    // =========================================================================

    @Test
    @DisplayName("createFlag - throws EntityNotFoundException when flag type is inactive")
    void createFlagThrowsEntityNotFoundWhenFlagTypeIsInactive() {
        // Arrange
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_CLIENTS_ALL");

        final RiskFlagType inactiveType = inactiveFlagType("Deprecated Risk");
        when(flagTypeRepository.findById(flagTypeId)).thenReturn(Optional.of(inactiveType));

        final CreateRiskFlagRequest request = new CreateRiskFlagRequest(
                flagTypeId, null, LocalDate.now().plusDays(7));

        // Act & Assert
        assertThatThrownBy(() -> buildService().createFlag(clientId, request, actorId, "Dr. Smith"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(flagRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // =========================================================================
    // resolveFlag — happy path
    // =========================================================================

    @Test
    @DisplayName("resolveFlag - calls flag.resolve() and appends audit on happy path")
    void resolveFlagCallsResolveAndAppendsAuditWhenFlagIsActive() {
        // Arrange
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_CLIENTS_ALL");

        final UUID flagId = UUID.randomUUID();
        final ClientRiskFlag flag = makeFlag(clientId, flagTypeId, actorId);
        ReflectionTestUtils.setField(flag, "id", flagId);
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        final RiskFlagType flagType = activeFlagType("Safeguarding Concern");
        when(flagTypeRepository.findById(flagTypeId)).thenReturn(Optional.of(flagType));

        when(flagRepository.save(any(ClientRiskFlag.class))).thenReturn(flag);
        when(auditLogRepository.save(any(RiskFlagAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final ResolveRiskFlagRequest request = new ResolveRiskFlagRequest("Risk mitigated");

        // Act
        final RiskFlagResponse response = buildService()
                .resolveFlag(clientId, flagId, request, actorId, "Dr. Smith");

        // Assert
        assertThat(response).isNotNull();
        assertThat(flag.getStatus()).isEqualTo(ClientRiskFlagStatus.RESOLVED);
        assertThat(flag.getResolutionNote()).isEqualTo("Risk mitigated");
        verify(flagRepository, times(1)).save(flag);
        verify(auditLogRepository, times(1)).save(any(RiskFlagAuditLog.class));
    }

    // =========================================================================
    // resolveFlag — already resolved
    // =========================================================================

    @Test
    @DisplayName("resolveFlag - throws 422 ResponseStatusException when flag is already resolved")
    void resolveFlagThrows422WhenFlagAlreadyResolved() {
        // Arrange
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_CLIENTS_ALL");

        final UUID flagId = UUID.randomUUID();
        final ClientRiskFlag flag = makeFlag(clientId, flagTypeId, actorId);
        ReflectionTestUtils.setField(flag, "id", flagId);
        flag.resolve(actorId, "Already done");

        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        final ResolveRiskFlagRequest request = new ResolveRiskFlagRequest("Again?");

        // Act & Assert
        assertThatThrownBy(() -> buildService().resolveFlag(clientId, flagId, request, actorId, "Dr. Smith"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(flagRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // =========================================================================
    // resolveFlag — flag belongs to different client
    // =========================================================================

    @Test
    @DisplayName("resolveFlag - throws EntityNotFoundException when flag belongs to different client")
    void resolveFlagThrowsEntityNotFoundWhenFlagBelongsToDifferentClient() {
        // Arrange
        setAuth(actorId, "MANAGE_RISK_FLAGS", "READ_CLIENTS_ALL");

        final UUID flagId = UUID.randomUUID();
        final UUID differentClientId = UUID.randomUUID();
        final ClientRiskFlag flag = makeFlag(differentClientId, flagTypeId, actorId);
        ReflectionTestUtils.setField(flag, "id", flagId);

        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        final ResolveRiskFlagRequest request = new ResolveRiskFlagRequest("Resolved");

        // Act & Assert — clientId is different from flag.getClientId()
        assertThatThrownBy(() -> buildService().resolveFlag(clientId, flagId, request, actorId, "Dr. Smith"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(flagRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // =========================================================================
    // listActiveFlags — strips clinicalNote when caller lacks READ_RISK_FLAG_NOTES
    // =========================================================================

    @Test
    @DisplayName("listActiveFlags - strips clinicalNote when caller lacks READ_RISK_FLAG_NOTES")
    void listActiveFlagsStripsClinicalNoteWhenCallerLacksReadRiskFlagNotes() {
        // Arrange — RECEPTION_ADMIN_STAFF: READ_RISK_FLAGS but NOT READ_RISK_FLAG_NOTES
        setAuth(actorId, "READ_RISK_FLAGS");

        final ClientRiskFlag flag = makeFlag(clientId, flagTypeId, actorId);
        ReflectionTestUtils.setField(flag, "clinicalNote", "Sensitive note");
        when(flagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of(flag));
        when(flagTypeRepository.findAllById(Set.of(flagTypeId)))
                .thenReturn(List.of(activeFlagType("Self-Harm Risk")));

        // Act
        final List<RiskFlagResponse> responses = buildService().listActiveFlags(clientId);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).clinicalNote()).isNull();
    }

    // =========================================================================
    // listActiveFlags — includes clinicalNote when caller has READ_RISK_FLAG_NOTES
    // =========================================================================

    @Test
    @DisplayName("listActiveFlags - includes clinicalNote when caller has READ_RISK_FLAG_NOTES")
    void listActiveFlagsIncludesClinicalNoteWhenCallerHasReadRiskFlagNotes() {
        // Arrange — THERAPIST: READ_RISK_FLAGS + READ_RISK_FLAG_NOTES
        setAuth(actorId, "READ_RISK_FLAGS", "READ_RISK_FLAG_NOTES");

        final ClientRiskFlag flag = makeFlag(clientId, flagTypeId, actorId);
        ReflectionTestUtils.setField(flag, "clinicalNote", "Sensitive note");
        when(flagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of(flag));
        when(flagTypeRepository.findAllById(Set.of(flagTypeId)))
                .thenReturn(List.of(activeFlagType("Self-Harm Risk")));

        // Act
        final List<RiskFlagResponse> responses = buildService().listActiveFlags(clientId);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).clinicalNote()).isEqualTo("Sensitive note");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setAuth(final UUID subject, final String... authorities) {
        final List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        subject.toString(),
                        "N/A",
                        grantedAuthorities
                )
        );
    }

    private RiskFlagType activeFlagType(final String name) {
        final RiskFlagType flagType = new RiskFlagType(name, (short) 1);
        ReflectionTestUtils.setField(flagType, "id", flagTypeId);
        return flagType;
    }

    private RiskFlagType inactiveFlagType(final String name) {
        final RiskFlagType flagType = new RiskFlagType(name, (short) 99);
        ReflectionTestUtils.setField(flagType, "id", flagTypeId);
        flagType.deactivate();
        return flagType;
    }

    private ClientRiskFlag makeFlag(final UUID cId, final UUID ftId, final UUID createdBy) {
        return new ClientRiskFlag(cId, ftId, null, LocalDate.now().plusDays(30), createdBy);
    }

    private Client makeClient(final UUID id, final UUID assignedTherapistId) {
        final Client client = new Client("Test Client");
        client.setAssignedTherapistId(assignedTherapistId);
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }
}
