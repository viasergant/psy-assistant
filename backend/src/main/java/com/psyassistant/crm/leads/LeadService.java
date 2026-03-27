package com.psyassistant.crm.leads;

import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.crm.leads.dto.ContactMethodRequest;
import com.psyassistant.crm.leads.dto.CreateLeadRequest;
import com.psyassistant.crm.leads.dto.LeadDetailDto;
import com.psyassistant.crm.leads.dto.LeadPageResponse;
import com.psyassistant.crm.leads.dto.LeadSummaryDto;
import com.psyassistant.crm.leads.dto.UpdateLeadRequest;
import com.psyassistant.users.UserManagementService;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing CRM lead records.
 *
 * <p>All state-changing operations produce audit log entries via {@link AuditLogService}.
 * The {@code lastContactDate} field is system-controlled and is updated automatically
 * when a lead transitions out of NEW status.
 */
@Service
public class LeadService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadService.class);
    private static final String OUTCOME_SUCCESS = "SUCCESS";

    // Audit event type constants
    private static final String EVT_LEAD_CREATED = "LEAD_CREATED";
    private static final String EVT_LEAD_UPDATED = "LEAD_UPDATED";
    private static final String EVT_LEAD_STATUS_CHANGED = "LEAD_STATUS_CHANGED";
    private static final String EVT_LEAD_ARCHIVED = "LEAD_ARCHIVED";

    /** Maximum length for truncated values in audit log change entries. */
    private static final int AUDIT_TRUNCATE_LEN = 100;

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * Constructs the service.
     *
     * @param leadRepository lead data access
     * @param userRepository user data access (for resolving owner display names)
     * @param auditLogService audit recorder
     */
    public LeadService(
            final LeadRepository leadRepository,
            final UserRepository userRepository,
            final AuditLogService auditLogService) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    // ---- queries ----------------------------------------------------------

    /**
     * Returns a paginated, optionally filtered list of leads.
     *
     * @param status          optional status filter
     * @param ownerId         optional owner filter
     * @param includeArchived when false, INACTIVE leads are excluded
     * @param pageable        pagination and sort parameters
     * @return page response with summary DTOs
     */
    @Transactional(readOnly = true)
    public LeadPageResponse listLeads(
            final LeadStatus status,
            final UUID ownerId,
            final boolean includeArchived,
            final Pageable pageable) {

        Page<Lead> page = leadRepository.findAll(
                LeadSpecification.withFilters(status, ownerId, includeArchived), pageable);

        // Batch-resolve owner names for all leads on this page
        Set<UUID> ownerIds = page.getContent().stream()
                .filter(l -> l.getOwnerId() != null)
                .map(Lead::getOwnerId)
                .collect(Collectors.toSet());
        Map<UUID, String> ownerNames = resolveOwnerNames(ownerIds);

        List<LeadSummaryDto> content = page.getContent().stream()
                .map(l -> LeadSummaryDto.from(l, ownerNames.get(l.getOwnerId())))
                .toList();

        return new LeadPageResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    /**
     * Returns the full detail view of a single lead.
     *
     * @param id the lead's UUID
     * @return detail DTO
     * @throws EntityNotFoundException if no lead exists with the given ID
     */
    @Transactional(readOnly = true)
    public LeadDetailDto getLead(final UUID id) {
        Lead lead = findById(id);
        String ownerName = resolveOwnerName(lead.getOwnerId());
        return LeadDetailDto.from(lead, ownerName);
    }

    // ---- mutations --------------------------------------------------------

    /**
     * Creates a new lead with the given contact methods.
     *
     * @param request  validated creation payload
     * @param actorId  UUID of the principal performing the action (for audit)
     * @return the created lead detail DTO
     */
    @Transactional
    public LeadDetailDto createLead(final CreateLeadRequest request, final UUID actorId) {
        Lead lead = new Lead(request.fullName());
        lead.setSource(request.source());
        lead.setOwnerId(request.ownerId());
        lead.setNotes(request.notes());

        List<LeadContactMethod> methods = buildContactMethods(lead, request.contactMethods());
        lead.replaceContactMethods(methods);

        leadRepository.save(lead);

        auditLogService.record(new AuditLog.Builder(EVT_LEAD_CREATED)
                .userId(actorId)
                .outcome(OUTCOME_SUCCESS)
                .detail("{\"event\":\"LEAD_CREATED\",\"leadId\":\"" + lead.getId() + "\"}")
                .build());

        LOG.info("event=LEAD_CREATED actorId={} leadId={}", actorId, lead.getId());

        String ownerName = resolveOwnerName(lead.getOwnerId());
        return LeadDetailDto.from(lead, ownerName);
    }

    /**
     * Fully replaces an existing lead's data (contact methods are replaced entirely).
     *
     * @param id       the lead's UUID
     * @param request  validated update payload
     * @param actorId  UUID of the principal performing the action (for audit)
     * @return the updated lead detail DTO
     * @throws EntityNotFoundException if no lead exists with the given ID
     */
    @Transactional
    public LeadDetailDto updateLead(
            final UUID id, final UpdateLeadRequest request, final UUID actorId) {

        Lead lead = findById(id);

        // Capture changed fields for audit (compact, truncated)
        String auditChanges = buildChangeSummary(lead, request);

        lead.setFullName(request.fullName());
        lead.setSource(request.source());
        lead.setOwnerId(request.ownerId());
        lead.setNotes(request.notes());

        List<LeadContactMethod> methods = buildContactMethods(lead, request.contactMethods());
        lead.replaceContactMethods(methods);

        leadRepository.save(lead);

        auditLogService.record(new AuditLog.Builder(EVT_LEAD_UPDATED)
                .userId(actorId)
                .outcome(OUTCOME_SUCCESS)
                .detail("{\"event\":\"LEAD_UPDATED\",\"leadId\":\"" + id + "\",\"changed\":"
                        + auditChanges + "}")
                .build());

        LOG.info("event=LEAD_UPDATED actorId={} leadId={}", actorId, id);

        String ownerName = resolveOwnerName(lead.getOwnerId());
        return LeadDetailDto.from(lead, ownerName);
    }

    /**
     * Transitions a lead to a new status using the FSM defined in {@link LeadStatus}.
     *
     * <p>The {@code lastContactDate} is updated automatically when transitioning to CONTACTED.
     *
     * @param id       the lead's UUID
     * @param target   the desired next status
     * @param actorId  UUID of the principal performing the action (for audit)
     * @return the updated lead detail DTO
     * @throws EntityNotFoundException        if no lead exists with the given ID
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    @Transactional
    public LeadDetailDto transitionStatus(
            final UUID id, final LeadStatus target, final UUID actorId) {

        Lead lead = findById(id);
        LeadStatus from = lead.getStatus();

        if (!from.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(from, target);
        }

        lead.setStatus(target);

        // Update lastContactDate when the lead has been contacted for the first time
        if (target == LeadStatus.CONTACTED) {
            lead.setLastContactDate(Instant.now());
        }

        leadRepository.save(lead);

        auditLogService.record(new AuditLog.Builder(EVT_LEAD_STATUS_CHANGED)
                .userId(actorId)
                .outcome(OUTCOME_SUCCESS)
                .detail("{\"event\":\"LEAD_STATUS_CHANGED\",\"leadId\":\"" + id
                        + "\",\"from\":\"" + from + "\",\"to\":\"" + target + "\"}")
                .build());

        LOG.info("event=LEAD_STATUS_CHANGED actorId={} leadId={} from={} to={}",
                actorId, id, from, target);

        String ownerName = resolveOwnerName(lead.getOwnerId());
        return LeadDetailDto.from(lead, ownerName);
    }

    /**
     * Archives a lead by transitioning it to INACTIVE.
     *
     * <p>Delegates to {@link #transitionStatus} — the same FSM rules apply.
     *
     * @param id      the lead's UUID
     * @param actorId UUID of the principal performing the action (for audit)
     * @return the archived lead detail DTO
     * @throws EntityNotFoundException        if no lead exists with the given ID
     * @throws InvalidStatusTransitionException if the lead is in a terminal state
     */
    @Transactional
    public LeadDetailDto archiveLead(final UUID id, final UUID actorId) {
        Lead lead = findById(id);
        LeadStatus from = lead.getStatus();

        if (!from.canTransitionTo(LeadStatus.INACTIVE)) {
            throw new InvalidStatusTransitionException(from, LeadStatus.INACTIVE);
        }

        lead.setStatus(LeadStatus.INACTIVE);
        leadRepository.save(lead);

        auditLogService.record(new AuditLog.Builder(EVT_LEAD_ARCHIVED)
                .userId(actorId)
                .outcome(OUTCOME_SUCCESS)
                .detail("{\"event\":\"LEAD_ARCHIVED\",\"leadId\":\"" + id
                        + "\",\"from\":\"" + from + "\"}")
                .build());

        LOG.info("event=LEAD_ARCHIVED actorId={} leadId={} from={}", actorId, id, from);

        String ownerName = resolveOwnerName(lead.getOwnerId());
        return LeadDetailDto.from(lead, ownerName);
    }

    // ---- private helpers --------------------------------------------------

    private Lead findById(final UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found: " + id));
    }

    private List<LeadContactMethod> buildContactMethods(
            final Lead lead, final List<ContactMethodRequest> requests) {
        List<LeadContactMethod> methods = new ArrayList<>();
        for (ContactMethodRequest req : requests) {
            methods.add(new LeadContactMethod(lead, req.type(), req.value(), req.isPrimary()));
        }
        return methods;
    }

    private String resolveOwnerName(final UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return userRepository.findById(ownerId)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse(null);
    }

    private Map<UUID, String> resolveOwnerNames(final Set<UUID> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(
                        u -> u.getId(),
                        u -> u.getFullName() != null ? u.getFullName() : u.getEmail()
                ));
    }

    /**
     * Builds a compact JSON map of changed scalar fields for the audit log.
     * Only includes fields that have actually changed; values are truncated to
     * {@value AUDIT_TRUNCATE_LEN} characters.
     */
    private String buildChangeSummary(final Lead existing, final UpdateLeadRequest req) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        first = appendIfChanged(sb, first, "fullName",
                existing.getFullName(), req.fullName());
        first = appendIfChanged(sb, first, "source",
                existing.getSource(), req.source());
        first = appendIfChanged(sb, first, "ownerId",
                existing.getOwnerId() == null ? null : existing.getOwnerId().toString(),
                req.ownerId() == null ? null : req.ownerId().toString());
        appendIfChanged(sb, first, "notes",
                existing.getNotes(), req.notes());

        sb.append("}");
        return sb.toString();
    }

    private boolean appendIfChanged(
            final StringBuilder sb, final boolean first,
            final String field, final String oldVal, final String newVal) {
        boolean changed = !java.util.Objects.equals(oldVal, newVal);
        if (!changed) {
            return first;
        }
        if (!first) {
            sb.append(",");
        }
        sb.append("\"").append(field).append("\":[")
                .append("\"").append(truncate(oldVal)).append("\",")
                .append("\"").append(truncate(newVal)).append("\"]");
        return false;
    }

    private String truncate(final String value) {
        if (value == null) {
            return "";
        }
        return value.length() > AUDIT_TRUNCATE_LEN
                ? value.substring(0, AUDIT_TRUNCATE_LEN)
                : value;
    }
}
