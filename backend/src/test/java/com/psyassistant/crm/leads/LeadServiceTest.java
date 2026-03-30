package com.psyassistant.crm.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.crm.leads.dto.ContactMethodRequest;
import com.psyassistant.crm.leads.dto.ConvertLeadRequest;
import com.psyassistant.crm.leads.dto.ConvertLeadResponse;
import com.psyassistant.crm.leads.dto.CreateLeadRequest;
import com.psyassistant.crm.leads.dto.LeadDetailDto;
import com.psyassistant.crm.leads.dto.LeadPageResponse;
import com.psyassistant.crm.leads.dto.UpdateLeadRequest;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link LeadService}.
 *
 * <p>Covers: create, update with audit, status transitions (allowed and forbidden),
 * archive, and list with archive filter.
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private LeadService service;

    private UUID actorId;
    private UUID leadId;

    @BeforeEach
    void setUp() {
        service = new LeadService(leadRepository, clientRepository, userRepository, auditLogService);
        actorId = UUID.randomUUID();
        leadId = UUID.randomUUID();
    }

    // ---- createLead -------------------------------------------------------

    @Test
    void createLeadPersistsLeadAndContactMethods() {
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLeadRequest req = new CreateLeadRequest(
                "Jane Smith",
                List.of(new ContactMethodRequest("EMAIL", "jane@example.com", true)),
                "referral",
                null,
                null);

        LeadDetailDto dto = service.createLead(req, actorId);

        assertThat(dto.fullName()).isEqualTo("Jane Smith");
        assertThat(dto.status()).isEqualTo(LeadStatus.NEW);
        assertThat(dto.contactMethods()).hasSize(1);
        assertThat(dto.contactMethods().get(0).value()).isEqualTo("jane@example.com");
        verify(leadRepository).save(any(Lead.class));
        verify(auditLogService).record(any());
    }

    @Test
    void createLeadWithPhoneContactMethodPersists() {
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateLeadRequest req = new CreateLeadRequest(
                "Bob Brown",
                List.of(new ContactMethodRequest("PHONE", "+1-555-0100", true)),
                null, null, null);

        LeadDetailDto dto = service.createLead(req, actorId);

        assertThat(dto.contactMethods()).hasSize(1);
        assertThat(dto.contactMethods().get(0).type()).isEqualTo("PHONE");
    }

    // ---- updateLead -------------------------------------------------------

    @Test
    void updateLeadWritesAuditEntryWithChangedFields() {
        Lead lead = makeLead(leadId, "Old Name", LeadStatus.NEW);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeadRequest req = new UpdateLeadRequest(
                "New Name",
                List.of(new ContactMethodRequest("EMAIL", "new@example.com", true)),
                "website", null, "some notes");

        service.updateLead(leadId, req, actorId);

        verify(auditLogService).record(any());
    }

    @Test
    void updateLeadThrowsEntityNotFoundForUnknownId() {
        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

        UpdateLeadRequest req = new UpdateLeadRequest(
                "Name",
                List.of(new ContactMethodRequest("EMAIL", "a@b.com", true)),
                null, null, null);

        assertThatThrownBy(() -> service.updateLead(leadId, req, actorId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- transitionStatus -------------------------------------------------

    @Test
    void transitionStatusNewToContactedSucceeds() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.NEW);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadDetailDto dto = service.transitionStatus(leadId, LeadStatus.CONTACTED, actorId);

        assertThat(dto.status()).isEqualTo(LeadStatus.CONTACTED);
        assertThat(dto.lastContactDate()).isNotNull();
        verify(auditLogService).record(any());
    }

    @Test
    void transitionStatusContactedToQualifiedSucceeds() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.CONTACTED);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadDetailDto dto = service.transitionStatus(leadId, LeadStatus.QUALIFIED, actorId);

        assertThat(dto.status()).isEqualTo(LeadStatus.QUALIFIED);
    }

    @Test
    void transitionStatusContactedToNewThrows422() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.CONTACTED);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                service.transitionStatus(leadId, LeadStatus.NEW, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CONTACTED")
                .hasMessageContaining("NEW");

        verify(leadRepository, never()).save(any());
    }

    @Test
    void transitionStatusConvertedToAnyThrows422() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.CONVERTED);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                service.transitionStatus(leadId, LeadStatus.NEW, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void transitionStatusInactiveToAnyThrows422() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.INACTIVE);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() ->
                service.transitionStatus(leadId, LeadStatus.NEW, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ---- archiveLead ------------------------------------------------------

    @Test
    void archiveLeadSetsInactiveWritesAudit() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.NEW);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadDetailDto dto = service.archiveLead(leadId, actorId);

        assertThat(dto.status()).isEqualTo(LeadStatus.INACTIVE);
        verify(auditLogService).record(any());
    }

    @Test
    void archiveLeadConvertedLeadThrows422() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.CONVERTED);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service.archiveLead(leadId, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(leadRepository, never()).save(any());
    }

    @Test
    void archiveLeadAlreadyInactiveThrows422() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.INACTIVE);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service.archiveLead(leadId, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ---- listLeads --------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void listLeadsExcludesArchivedByDefault() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.NEW);
        addContactMethod(lead, "EMAIL", "jane@example.com", true);

        Page<Lead> page = new PageImpl<>(List.of(lead), PageRequest.of(0, 20), 1);
        when(leadRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        LeadPageResponse response = service.listLeads(null, null, false, PageRequest.of(0, 20));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).fullName()).isEqualTo("Jane");
        verify(leadRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void listLeadsIncludesArchivedWhenFlagSet() {
        Lead inactiveLead = makeLead(leadId, "Archive Jane", LeadStatus.INACTIVE);
        addContactMethod(inactiveLead, "PHONE", "+1-555-0100", true);

        Page<Lead> page = new PageImpl<>(List.of(inactiveLead), PageRequest.of(0, 20), 1);
        when(leadRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        LeadPageResponse response = service.listLeads(null, null, true, PageRequest.of(0, 20));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).status()).isEqualTo(LeadStatus.INACTIVE);
        verify(leadRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // ---- convertLead -------------------------------------------------------

    @Test
    void convertLeadHappyPathReturnsClientAndLeadIds() {
        Lead lead = makeLead(leadId, "Jane Smith", LeadStatus.QUALIFIED);
        UUID clientId = UUID.randomUUID();
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", clientId);
            return c;
        });
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane Smith",
                List.of(new ContactMethodRequest("EMAIL", "jane@example.com", true)),
                null, null);

        ConvertLeadResponse response = service.convertLead(leadId, req, actorId);

        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.leadId()).isEqualTo(leadId);
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
        assertThat(lead.getConvertedClientId()).isEqualTo(clientId);
        verify(clientRepository, times(2)).save(any(Client.class));
        verify(leadRepository).save(any(Lead.class));
        verify(auditLogService).record(any());
    }

    @Test
    void convertLeadOwnerDefaultsToLeadOwnerWhenNotProvided() {
        UUID leadOwner = UUID.randomUUID();
        Lead lead = makeLead(leadId, "Jane", LeadStatus.QUALIFIED);
        lead.setOwnerId(leadOwner);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("PHONE", "+1-555-0100", true)),
                null, null /* ownerId not provided */);

        service.convertLead(leadId, req, actorId);

        // The saved client should have the lead's owner
        verify(clientRepository, times(2)).save(any(Client.class));
    }

    @Test
    void convertLeadOwnerDefaultsToActorIdWhenLeadOwnerIsNull() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.QUALIFIED);
        // lead.ownerId is null
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            // Verify the resolved owner is actorId
            assertThat(c.getOwnerId()).isEqualTo(actorId);
            return c;
        });
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        service.convertLead(leadId, req, actorId);

        verify(clientRepository, times(2)).save(any(Client.class));
    }

    @Test
    void convertLeadThrows422WhenLeadIsNew() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.NEW);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        assertThatThrownBy(() -> service.convertLead(leadId, req, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("NEW")
                .hasMessageContaining("CONVERTED");

        verify(clientRepository, never()).save(any());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void convertLeadThrows422WhenLeadIsContacted() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.CONTACTED);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        assertThatThrownBy(() -> service.convertLead(leadId, req, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void convertLeadThrows422WhenLeadIsInactive() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.INACTIVE);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        assertThatThrownBy(() -> service.convertLead(leadId, req, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void convertLeadThrows409WhenStatusIsAlreadyConverted() {
        UUID existingClientId = UUID.randomUUID();
        Lead lead = makeLead(leadId, "Jane", LeadStatus.CONVERTED);
        lead.setConvertedClientId(existingClientId);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        assertThatThrownBy(() -> service.convertLead(leadId, req, actorId))
                .isInstanceOf(LeadAlreadyConvertedException.class)
                .satisfies(ex -> {
                    LeadAlreadyConvertedException e = (LeadAlreadyConvertedException) ex;
                    assertThat(e.getLeadId()).isEqualTo(leadId);
                    assertThat(e.getExistingClientId()).isEqualTo(existingClientId);
                });

        verify(clientRepository, never()).save(any());
        verify(leadRepository, never()).save(any());
    }

    @Test
    void convertLeadThrows409WhenDbConstraintViolatesSourceLeadId() {
        Lead lead = makeLead(leadId, "Jane", LeadStatus.QUALIFIED);
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        // Simulate DB unique constraint violation on source_lead_id
        DataIntegrityViolationException dbEx = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint on source_lead_id");
        when(clientRepository.save(any(Client.class))).thenThrow(dbEx);

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        assertThatThrownBy(() -> service.convertLead(leadId, req, actorId))
                .isInstanceOf(LeadAlreadyConvertedException.class)
                .satisfies(ex -> {
                    LeadAlreadyConvertedException e = (LeadAlreadyConvertedException) ex;
                    assertThat(e.getLeadId()).isEqualTo(leadId);
                    assertThat(e.getExistingClientId()).isNull();
                });

        // Lead must NOT have been saved (rollback)
        verify(leadRepository, never()).save(any());
    }

    @Test
    void convertLeadThrowsEntityNotFoundForUnknownId() {
        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

        ConvertLeadRequest req = new ConvertLeadRequest(
                "Jane",
                List.of(new ContactMethodRequest("EMAIL", "j@example.com", true)),
                null, null);

        assertThatThrownBy(() -> service.convertLead(leadId, req, actorId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(clientRepository, never()).save(any());
    }

    // ---- helpers ----------------------------------------------------------

    private Lead makeLead(final UUID id, final String fullName, final LeadStatus status) {
        Lead lead = new Lead(fullName);
        lead.setStatus(status);
        ReflectionTestUtils.setField(lead, "id", id);
        return lead;
    }

    private void addContactMethod(
            final Lead lead, final String type, final String value, final boolean primary) {
        LeadContactMethod cm = new LeadContactMethod(lead, type, value, primary);
        ReflectionTestUtils.setField(cm, "id", UUID.randomUUID());
        @SuppressWarnings("unchecked")
        java.util.List<LeadContactMethod> methods =
                (java.util.List<LeadContactMethod>) ReflectionTestUtils.getField(lead, "contactMethods");
        if (methods != null) {
            methods.add(cm);
        }
    }
}
