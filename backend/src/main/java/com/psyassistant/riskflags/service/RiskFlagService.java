package com.psyassistant.riskflags.service;

import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.riskflags.domain.ClientRiskFlag;
import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import com.psyassistant.riskflags.domain.RiskFlagAuditActionType;
import com.psyassistant.riskflags.domain.RiskFlagAuditLog;
import com.psyassistant.riskflags.domain.RiskFlagType;
import com.psyassistant.riskflags.dto.CreateRiskFlagRequest;
import com.psyassistant.riskflags.dto.ResolveRiskFlagRequest;
import com.psyassistant.riskflags.dto.RiskFlagResponse;
import com.psyassistant.riskflags.repository.ClientRiskFlagRepository;
import com.psyassistant.riskflags.repository.RiskFlagAuditLogRepository;
import com.psyassistant.riskflags.repository.RiskFlagTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Core service for risk flag lifecycle management.
 *
 * <p>RBAC rules:
 * <ul>
 *   <li>Create and resolve: {@code MANAGE_RISK_FLAGS} (THERAPIST, SUPERVISOR, SYS_ADMIN)</li>
 *   <li>List active flags: {@code READ_RISK_FLAGS} (all clinical staff + RECEPTION_ADMIN_STAFF)</li>
 *   <li>List full history: {@code READ_RISK_FLAG_NOTES} (THERAPIST, SUPERVISOR, SYS_ADMIN)</li>
 * </ul>
 *
 * <p>Assignment check: callers with {@code READ_ASSIGNED_CLIENTS} but without
 * {@code READ_CLIENTS_ALL} are subject to an assignment check — they may only manage
 * flags for clients assigned to them.
 */
@Service
public class RiskFlagService {

    private static final Logger LOG = LoggerFactory.getLogger(RiskFlagService.class);

    private final ClientRiskFlagRepository flagRepository;
    private final RiskFlagTypeRepository flagTypeRepository;
    private final RiskFlagAuditLogRepository auditLogRepository;
    private final ClientRepository clientRepository;

    public RiskFlagService(
            final ClientRiskFlagRepository flagRepository,
            final RiskFlagTypeRepository flagTypeRepository,
            final RiskFlagAuditLogRepository auditLogRepository,
            final ClientRepository clientRepository) {
        this.flagRepository = flagRepository;
        this.flagTypeRepository = flagTypeRepository;
        this.auditLogRepository = auditLogRepository;
        this.clientRepository = clientRepository;
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Raises a new risk flag on a client profile.
     *
     * @param clientId  client to flag
     * @param request   flag creation payload
     * @param actorId   user raising the flag
     * @param actorName display name of the actor
     * @return the persisted flag as a response DTO
     * @throws AccessDeniedException   if caller lacks {@code MANAGE_RISK_FLAGS} or is an
     *                                  unassigned THERAPIST
     * @throws EntityNotFoundException if the flag type is not found or is inactive
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAGS')")
    public RiskFlagResponse createFlag(
            final UUID clientId,
            final CreateRiskFlagRequest request,
            final UUID actorId,
            final String actorName) {

        requireAuthority("MANAGE_RISK_FLAGS");
        enforceTherapistAssignmentCheck(clientId, actorId);

        final RiskFlagType flagType = loadActiveFlagType(request.flagTypeId());

        final ClientRiskFlag flag = new ClientRiskFlag(
                clientId,
                request.flagTypeId(),
                request.clinicalNote(),
                request.reviewDate(),
                actorId
        );
        final ClientRiskFlag saved = flagRepository.save(flag);

        auditLogRepository.save(new RiskFlagAuditLog(
                saved.getId(),
                clientId,
                actorId,
                actorName,
                RiskFlagAuditActionType.FLAG_CREATED,
                flagType.getName(),
                ClientRiskFlagStatus.ACTIVE.name()
        ));

        LOG.info("Risk flag created: id={}, client={}, type={}, by={}",
                saved.getId(), clientId, flagType.getName(), actorId);

        return toResponse(saved, flagType.getName());
    }

    // =========================================================================
    // Resolve
    // =========================================================================

    /**
     * Resolves an active risk flag.
     *
     * @param clientId  client owning the flag (used to verify ownership)
     * @param flagId    UUID of the flag to resolve
     * @param request   resolution payload
     * @param actorId   user resolving the flag
     * @param actorName display name of the actor
     * @return the updated flag as a response DTO
     * @throws AccessDeniedException       if caller lacks {@code MANAGE_RISK_FLAGS} or is
     *                                      an unassigned THERAPIST
     * @throws EntityNotFoundException     if the flag is not found or belongs to a different client
     * @throws ResponseStatusException     with 422 if the flag is already resolved
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAGS')")
    public RiskFlagResponse resolveFlag(
            final UUID clientId,
            final UUID flagId,
            final ResolveRiskFlagRequest request,
            final UUID actorId,
            final String actorName) {

        requireAuthority("MANAGE_RISK_FLAGS");

        final ClientRiskFlag flag = flagRepository.findById(flagId)
                .filter(f -> f.getClientId().equals(clientId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Risk flag not found: " + flagId + " for client: " + clientId));

        if (flag.getStatus() == ClientRiskFlagStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Flag is already resolved");
        }

        enforceTherapistAssignmentCheck(clientId, actorId);

        final RiskFlagType flagType = loadFlagTypeById(flag.getFlagTypeId());

        flag.resolve(actorId, request.resolutionNote());
        final ClientRiskFlag saved = flagRepository.save(flag);

        auditLogRepository.save(new RiskFlagAuditLog(
                saved.getId(),
                clientId,
                actorId,
                actorName,
                RiskFlagAuditActionType.FLAG_RESOLVED,
                flagType.getName(),
                ClientRiskFlagStatus.RESOLVED.name()
        ));

        LOG.info("Risk flag resolved: id={}, client={}, by={}", flagId, clientId, actorId);

        return toResponse(saved, flagType.getName());
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Returns all active risk flags for a client.
     * Strips {@code clinicalNote} for callers lacking {@code READ_RISK_FLAG_NOTES}.
     * Requires {@code READ_RISK_FLAGS}.
     *
     * @param clientId client to query
     * @return list of active flags with notes stripped when caller lacks the notes permission
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_RISK_FLAGS')")
    public List<RiskFlagResponse> listActiveFlags(final UUID clientId) {
        requireAuthority("READ_RISK_FLAGS");
        final boolean includeNotes = hasAuthority("READ_RISK_FLAG_NOTES");

        final List<ClientRiskFlag> flags =
                flagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE);

        final Set<UUID> flagTypeIds = flags.stream()
                .map(ClientRiskFlag::getFlagTypeId)
                .collect(Collectors.toSet());
        final Map<UUID, String> flagTypeNames = flagTypeRepository.findAllById(flagTypeIds).stream()
                .collect(Collectors.toMap(RiskFlagType::getId, RiskFlagType::getName));

        return flags.stream()
                .map(flag -> {
                    final String flagTypeName =
                            flagTypeNames.getOrDefault(flag.getFlagTypeId(), "Unknown");
                    final String clinicalNote = includeNotes ? flag.getClinicalNote() : null;
                    return toResponseWithNote(flag, flagTypeName, clinicalNote);
                })
                .toList();
    }

    /**
     * Returns the full risk flag history for a client, newest first.
     * Requires {@code READ_RISK_FLAG_NOTES} (supervisor/SYS_ADMIN only).
     *
     * @param clientId client to query
     * @return all flags (active and resolved) with clinical notes included
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_RISK_FLAG_NOTES')")
    public List<RiskFlagResponse> listAllFlags(final UUID clientId) {
        requireAuthority("READ_RISK_FLAG_NOTES");

        final List<ClientRiskFlag> flags =
                flagRepository.findAllByClientIdOrderByCreatedAtDesc(clientId);

        final Set<UUID> flagTypeIds = flags.stream()
                .map(ClientRiskFlag::getFlagTypeId)
                .collect(Collectors.toSet());
        final Map<UUID, String> flagTypeNames = flagTypeRepository.findAllById(flagTypeIds).stream()
                .collect(Collectors.toMap(RiskFlagType::getId, RiskFlagType::getName));

        return flags.stream()
                .map(flag -> {
                    final String flagTypeName =
                            flagTypeNames.getOrDefault(flag.getFlagTypeId(), "Unknown");
                    return toResponse(flag, flagTypeName);
                })
                .toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Enforces the THERAPIST assignment check.
     * If the caller has {@code READ_ASSIGNED_CLIENTS} but NOT {@code READ_CLIENTS_ALL},
     * the client must be assigned to the actor.
     */
    private void enforceTherapistAssignmentCheck(final UUID clientId, final UUID actorId) {
        if (hasAuthority("READ_ASSIGNED_CLIENTS") && !hasAuthority("READ_CLIENTS_ALL")) {
            final com.psyassistant.crm.clients.Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientId));
            if (!Objects.equals(actorId, client.getAssignedTherapistId())) {
                throw new AccessDeniedException(
                        "Therapist is not assigned to client: " + clientId);
            }
        }
    }

    private RiskFlagType loadActiveFlagType(final UUID flagTypeId) {
        final RiskFlagType flagType = flagTypeRepository.findById(flagTypeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Risk flag type not found: " + flagTypeId));
        if (!flagType.isActive()) {
            throw new EntityNotFoundException(
                    "Risk flag type is inactive: " + flagTypeId);
        }
        return flagType;
    }

    private RiskFlagType loadFlagTypeById(final UUID flagTypeId) {
        return flagTypeRepository.findById(flagTypeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Risk flag type not found: " + flagTypeId));
    }

    private void requireAuthority(final String authority) {
        if (!hasAuthority(authority)) {
            throw new AccessDeniedException("Access denied: missing authority " + authority);
        }
    }

    private boolean hasAuthority(final String authority) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private RiskFlagResponse toResponse(final ClientRiskFlag flag, final String flagTypeName) {
        return new RiskFlagResponse(
                flag.getId(),
                flag.getClientId(),
                flag.getFlagTypeId(),
                flagTypeName,
                flag.getStatus(),
                flag.getClinicalNote(),
                flag.getReviewDate(),
                flag.getCreatedByUserId(),
                flag.getCreatedAt(),
                flag.getResolvedByUserId(),
                flag.getResolvedAt(),
                flag.getResolutionNote()
        );
    }

    private RiskFlagResponse toResponseWithNote(
            final ClientRiskFlag flag,
            final String flagTypeName,
            final String clinicalNote) {
        return new RiskFlagResponse(
                flag.getId(),
                flag.getClientId(),
                flag.getFlagTypeId(),
                flagTypeName,
                flag.getStatus(),
                clinicalNote,
                flag.getReviewDate(),
                flag.getCreatedByUserId(),
                flag.getCreatedAt(),
                flag.getResolvedByUserId(),
                flag.getResolvedAt(),
                flag.getResolutionNote()
        );
    }
}
