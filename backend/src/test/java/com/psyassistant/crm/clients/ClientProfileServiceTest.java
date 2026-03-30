package com.psyassistant.crm.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.crm.clients.audit.ClientProfileAuditRecorder;
import com.psyassistant.crm.clients.dto.ClientDetailDto;
import com.psyassistant.crm.clients.dto.UpdateClientProfileRequest;
import com.psyassistant.crm.clients.dto.UpdateClientTagsRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ClientProfileService}.
 */
@ExtendWith(MockitoExtension.class)
class ClientProfileServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientProfileAuditRecorder auditRecorder;

    @Mock
    private ClientTagRepository clientTagRepository;

    @Mock
    private ClientPhotoStorage clientPhotoStorage;

    private ClientProfileService service;

    private final UUID clientId = UUID.randomUUID();
    private final UUID therapistId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ClientProfileService(
                clientRepository,
                clientTagRepository,
                auditRecorder,
                clientPhotoStorage,
                5_242_880L
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getClientProfileForManageClientsReturnsEditableCapabilities() {
        Client client = makeClient(clientId, 4L, therapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientTagRepository.findAllByClientId(clientId)).thenReturn(List.of());
        setAuth(UUID.randomUUID(), "MANAGE_CLIENTS");

        ClientDetailDto dto = service.getClientProfile(clientId);

        assertThat(dto.id()).isEqualTo(clientId);
        assertThat(dto.canEditProfile()).isTrue();
        assertThat(dto.canEditTags()).isTrue();
        assertThat(dto.canUploadPhoto()).isTrue();
    }

    @Test
    void getClientProfileForAssignedTherapistReturnsReadOnly() {
        Client client = makeClient(clientId, 2L, therapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientTagRepository.findAllByClientId(clientId)).thenReturn(List.of());
        setAuth(therapistId, "READ_ASSIGNED_CLIENTS");

        ClientDetailDto dto = service.getClientProfile(clientId);

        assertThat(dto.canEditProfile()).isFalse();
        assertThat(dto.id()).isEqualTo(clientId);
    }

    @Test
    void getClientProfileForUnassignedTherapistThrowsAccessDenied() {
        Client client = makeClient(clientId, 2L, therapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        setAuth(UUID.randomUUID(), "READ_ASSIGNED_CLIENTS");

        assertThatThrownBy(() -> service.getClientProfile(clientId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateClientProfileWithoutManageClientsThrowsAccessDenied() {
        setAuth(UUID.randomUUID(), "READ_CLIENTS_ALL");

        assertThatThrownBy(() -> service.updateClientProfile(
                clientId,
                updateRequest(1L, "Updated"),
                UUID.randomUUID(),
                "tester"))
                .isInstanceOf(AccessDeniedException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateClientProfileWithStaleVersionThrowsConflict() {
        Client client = makeClient(clientId, 3L, therapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        setAuth(UUID.randomUUID(), "MANAGE_CLIENTS");

        assertThatThrownBy(() -> service.updateClientProfile(
                clientId,
                updateRequest(2L, "Updated"),
                UUID.randomUUID(),
                "tester"))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void updateClientProfileWritesAuditWhenFieldsChange() {
        UUID actorId = UUID.randomUUID();
        Client client = makeClient(clientId, 1L, therapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientTagRepository.findAllByClientId(clientId)).thenReturn(List.of());
        setAuth(actorId, "MANAGE_CLIENTS");

        ClientDetailDto dto = service.updateClientProfile(
                clientId,
                updateRequest(1L, "Anna Updated"),
                actorId,
                actorId.toString());

        assertThat(dto.fullName()).isEqualTo("Anna Updated");
        verify(clientRepository).save(any(Client.class));
        verify(auditRecorder).recordProfileUpdate(any(), any(), any(), any());
    }

    @Test
    void getClientProfileUnknownIdThrowsNotFound() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());
        setAuth(UUID.randomUUID(), "MANAGE_CLIENTS");

        assertThatThrownBy(() -> service.getClientProfile(clientId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateClientTagsReplacesTagSet() {
        UUID actorId = UUID.randomUUID();
        Client client = makeClient(clientId, 2L, therapistId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientTagRepository.findAllByClientId(clientId)).thenReturn(List.of());
        setAuth(actorId, "MANAGE_CLIENTS");

        ClientDetailDto dto = service.updateClientTags(
                clientId,
                new UpdateClientTagsRequest(2L, List.of("adult", "priority")),
                actorId,
                "actor"
        );

        assertThat(dto.tags()).containsExactly("adult", "priority");
        verify(clientTagRepository).deleteAllByClientId(clientId);
        verify(clientTagRepository).saveAll(any());
    }

    private Client makeClient(final UUID id, final Long version, final UUID assignedTherapistId) {
        Client client = new Client("Anna Kovalenko");
        client.setAssignedTherapistId(assignedTherapistId);
        ReflectionTestUtils.setField(client, "id", id);
        ReflectionTestUtils.setField(client, "version", version);
        return client;
    }

    private UpdateClientProfileRequest updateRequest(final Long version, final String fullName) {
        return new UpdateClientProfileRequest(
                version,
                fullName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void setAuth(final UUID subject, final String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        subject.toString(),
                        "N/A",
                        java.util.List.of(new SimpleGrantedAuthority(authority))
                )
        );
    }
}
