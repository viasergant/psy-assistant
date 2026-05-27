package com.psyassistant.billing.pkg;

import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.billing.pkg.dto.CreatePackageDefinitionRequest;
import com.psyassistant.billing.pkg.dto.PackageDefinitionResponse;
import com.psyassistant.billing.pkg.dto.PackageInstanceResponse;
import com.psyassistant.billing.pkg.dto.SellPackageRequest;
import com.psyassistant.billing.pkg.dto.UpdatePackageDefinitionStatusRequest;
import com.psyassistant.notifications.BillingNotificationPort;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for prepaid package definitions and client package instances.
 *
 * <p>RBAC:
 * <ul>
 *   <li>MANAGE_PACKAGES — Finance Staff and System Administrator</li>
 *   <li>READ_PACKAGES   — Finance Staff, System Administrator, Supervisor</li>
 * </ul>
 *
 * <p>Session deduction ({@link #tryDeductSession}) carries no {@code @PreAuthorize}
 * because it is called internally from the session-completion flow, not directly
 * from a controller.
 */
@Service
public class PackageService {

    private static final Logger LOG = LoggerFactory.getLogger(PackageService.class);

    private final PrepaidPackageDefinitionRepository definitionRepository;
    private final PrepaidPackageInstanceRepository instanceRepository;
    private final PackageBalanceAuditRepository auditRepository;
    private final BillingNotificationPort notifications;

    public PackageService(
            final PrepaidPackageDefinitionRepository definitionRepository,
            final PrepaidPackageInstanceRepository instanceRepository,
            final PackageBalanceAuditRepository auditRepository,
            final BillingNotificationPort notifications) {
        this.definitionRepository = definitionRepository;
        this.instanceRepository = instanceRepository;
        this.auditRepository = auditRepository;
        this.notifications = notifications;
    }

    // =========================================================================
    // Package definitions
    // =========================================================================

    /**
     * Creates a new prepaid package definition.
     *
     * @param request   the creation request
     * @param actorName the authenticated user performing the action
     * @return the persisted definition
     * @throws IllegalArgumentException if a definition with the same name already exists
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    public PackageDefinitionResponse createDefinition(
            final CreatePackageDefinitionRequest request,
            final String actorName) {

        if (definitionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException(
                    "A package definition with name '" + request.name() + "' already exists.");
        }

        PrepaidPackageDefinition definition = new PrepaidPackageDefinition(
                request.name(),
                request.serviceType(),
                request.sessionQty(),
                request.price());
        definition.setUpdatedBy(actorName);

        PrepaidPackageDefinition saved = definitionRepository.save(definition);
        LOG.info("Package definition '{}' created by {}", saved.getName(), actorName);
        return PackageDefinitionResponse.from(saved);
    }

    /**
     * Updates the status of a package definition (e.g. ACTIVE → ARCHIVED).
     *
     * @param id        the definition UUID
     * @param request   the desired new status
     * @param actorName the authenticated user performing the action
     * @return the updated definition
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    public PackageDefinitionResponse updateDefinitionStatus(
            final UUID id,
            final UpdatePackageDefinitionStatusRequest request,
            final String actorName) {

        PrepaidPackageDefinition definition = loadDefinitionById(id);
        definition.setStatus(request.status());
        definition.setUpdatedBy(actorName);
        LOG.info("Package definition {} status set to {} by {}", id, request.status(), actorName);
        return PackageDefinitionResponse.from(definition);
    }

    /**
     * Lists package definitions, optionally filtered by status.
     *
     * @param status optional filter; when {@code null} all definitions are returned
     * @return matching definitions ordered by name
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_PACKAGES')")
    public List<PackageDefinitionResponse> listDefinitions(final PackageDefinitionStatus status) {
        List<PrepaidPackageDefinition> results = status != null
                ? definitionRepository.findByStatusOrderByName(status)
                : definitionRepository.findAll();
        return results.stream().map(PackageDefinitionResponse::from).toList();
    }

    // =========================================================================
    // Package instances (sales)
    // =========================================================================

    /**
     * Sells a prepaid package to a client, creating a new {@link PrepaidPackageInstance}.
     *
     * @param request   the sell request containing definition, client, purchase date, and
     *                  optional validity duration
     * @param actorName the authenticated user performing the action
     * @return the created package instance
     * @throws EntityNotFoundException  if the referenced definition does not exist
     * @throws IllegalStateException    if the definition is not ACTIVE
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    public PackageInstanceResponse sellPackage(
            final SellPackageRequest request,
            final String actorName) {

        PrepaidPackageDefinition definition = loadDefinitionById(request.definitionId());
        if (definition.getStatus() != PackageDefinitionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot sell a package with status " + definition.getStatus()
                    + ". Only ACTIVE definitions can be sold.");
        }

        LocalDate expiresAt = null;
        if (request.validityDays() != null && request.validityDays() > 0) {
            expiresAt = LocalDate.ofInstant(request.purchasedAt(), java.time.ZoneOffset.UTC)
                    .plusDays(request.validityDays());
        }

        PrepaidPackageInstance instance = new PrepaidPackageInstance(
                definition,
                request.clientId(),
                request.purchasedAt(),
                definition.getSessionQty(),
                expiresAt);
        instance.setUpdatedBy(actorName);

        PrepaidPackageInstance saved = instanceRepository.save(instance);
        LOG.info("Package '{}' sold to client {} (instance {}) by {}",
                definition.getName(), request.clientId(), saved.getId(), actorName);
        return PackageInstanceResponse.from(saved);
    }

    /**
     * Returns all package instances for a given client, ordered by purchase date ascending.
     *
     * @param clientId the client UUID
     * @return list of package instance responses
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_PACKAGES')")
    public List<PackageInstanceResponse> listClientPackages(final UUID clientId) {
        return instanceRepository.findByClientIdOrderByPurchasedAtAsc(clientId)
                .stream()
                .map(PackageInstanceResponse::from)
                .toList();
    }

    // =========================================================================
    // Session deduction (internal — called from session completion flow)
    // =========================================================================

    /**
     * Attempts to deduct one session from the oldest eligible active package for the given
     * client and service type using a pessimistic write lock (FIFO order).
     *
     * <p>This method is intentionally not annotated with {@code @PreAuthorize} because it is
     * called from the session-completion service, which already enforces its own access rules.
     *
     * @param clientId    the client whose package should be debited
     * @param serviceType the service type that must match the package definition
     * @param sessionId   the session being linked to the deduction audit entry
     * @param therapistId the therapist who conducted the session
     * @param actorName   the authenticated actor (for audit)
     * @return a sealed {@link PackageDeductionResult} indicating outcome
     */
    @Transactional
    public PackageDeductionResult tryDeductSession(
            final UUID clientId,
            final ServiceType serviceType,
            final UUID sessionId,
            final UUID therapistId,
            final String actorName) {

        var eligible = instanceRepository.findFirstEligibleForDeduction(
                clientId, serviceType, LocalDate.now());

        if (eligible.isEmpty()) {
            return new PackageDeductionResult.NoEligiblePackage();
        }

        PrepaidPackageInstance instance = eligible.get();
        int balanceBefore = instance.getSessionsRemaining();
        int balanceAfter = balanceBefore - 1;

        instance.setSessionsRemaining(balanceAfter);
        instance.setUpdatedBy(actorName);

        auditRepository.save(new PackageBalanceAudit(
                instance, sessionId, therapistId,
                balanceBefore, balanceAfter,
                BalanceAuditAction.DEDUCT, actorName));

        if (balanceAfter == 0) {
            instance.setStatus(PackageInstanceStatus.EXHAUSTED);
            LOG.info("Package instance {} exhausted for client {}", instance.getId(), clientId);
            try {
                notifications.notifyPackageExhausted(clientId, instance.getId());
            } catch (Exception ex) {
                LOG.warn("Package exhausted notification failed for instance {}: {}",
                        instance.getId(), ex.getMessage());
            }
            return new PackageDeductionResult.PackageExhausted(instance.getId());
        }

        return new PackageDeductionResult.Deducted(instance.getId(), balanceBefore, balanceAfter);
    }

    // =========================================================================
    // Expiry (called by scheduler)
    // =========================================================================

    /**
     * Marks all active package instances whose expiry date is before {@code asOf} as EXPIRED,
     * writing a {@link PackageBalanceAudit} entry for each.
     *
     * <p>Intended to be called from a scheduled job; not exposed as a REST endpoint.
     *
     * @param asOf the reference date — instances with {@code expiresAt < asOf} are expired
     */
    @Transactional
    public void expirePackages(final LocalDate asOf) {
        List<PrepaidPackageInstance> overdue = instanceRepository.findOverdue(asOf);
        for (PrepaidPackageInstance instance : overdue) {
            int remaining = instance.getSessionsRemaining();
            instance.setStatus(PackageInstanceStatus.EXPIRED);
            instance.setSessionsRemaining(0);
            instance.setUpdatedBy("scheduler");

            auditRepository.save(new PackageBalanceAudit(
                    instance, null, null,
                    remaining, 0,
                    BalanceAuditAction.EXPIRE, "scheduler"));

            try {
                notifications.notifyPackageExpired(instance.getClientId(), instance.getId(), remaining);
            } catch (Exception ex) {
                LOG.warn("Package expired notification failed for instance {}: {}", instance.getId(), ex.getMessage());
            }
        }
        if (!overdue.isEmpty()) {
            LOG.info("Expired {} package instances as of {}", overdue.size(), asOf);
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private PrepaidPackageDefinition loadDefinitionById(final UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Package definition not found: " + id));
    }
}
