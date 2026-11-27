package com.psyassistant.admin;

import com.psyassistant.auth.domain.RefreshTokenRepository;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.GlobalAdminProperties;
import com.psyassistant.common.config.PresenceProperties;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.users.SelfDeactivationException;
import com.psyassistant.users.User;
import com.psyassistant.users.UserHasHistoryException;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.UserSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-organization user queries and account lifecycle actions for the global administrator.
 */
@Service
public class GlobalAdminUserService {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalAdminUserService.class);

    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String EVENT_USER_DEACTIVATED = "USER_DEACTIVATED";
    private static final String EVENT_USER_REACTIVATED = "USER_REACTIVATED";
    private static final String EVENT_USER_DELETED = "USER_DELETED";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final GlobalAdminProperties globalAdminProperties;
    private final PresenceProperties presenceProperties;

    /**
     * Constructs the service.
     *
     * @param userRepository         user repository
     * @param organizationRepository organization repository
     * @param refreshTokenRepository refresh token repository
     * @param auditLogService        audit log service
     * @param globalAdminProperties  configured global-admin identity
     * @param presenceProperties     presence settings (online window)
     */
    public GlobalAdminUserService(
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository,
            final RefreshTokenRepository refreshTokenRepository,
            final AuditLogService auditLogService,
            final GlobalAdminProperties globalAdminProperties,
            final PresenceProperties presenceProperties) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.globalAdminProperties = globalAdminProperties;
        this.presenceProperties = presenceProperties;
    }

    /**
     * Lists users of all organizations with optional filters.
     *
     * @param role     optional role filter
     * @param active   optional active-status filter
     * @param search   optional case-insensitive email/full-name substring
     * @param pageable page request
     * @return page of users enriched with organization info
     */
    @Transactional(readOnly = true)
    public GlobalUserPageResponse listUsers(
            final UserRole role, final Boolean active, final String search, final Pageable pageable) {
        UserRole normalizedRole = role == null ? null : role.canonical();
        Page<User> page = userRepository.findAll(
                UserSpecification.withFilters(normalizedRole, active, search), pageable);

        List<UUID> orgIds = page.getContent().stream()
                .map(User::getOrgId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Organization> orgs = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, Function.identity()));

        Instant now = Instant.now();
        List<GlobalUserSummaryDto> content = page.getContent().stream()
                .map(u -> GlobalUserSummaryDto.from(
                        u, orgs.get(u.getOrgId()), presenceOf(u, now), blockReasonOf(u, now)))
                .toList();
        return new GlobalUserPageResponse(
                content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    /**
     * Returns summary counts (total, online, blocked) over all users of all organizations.
     *
     * @return the counts
     */
    @Transactional(readOnly = true)
    public GlobalUserStatsDto getStats() {
        Instant now = Instant.now();
        return new GlobalUserStatsDto(
                userRepository.countAllUsers(),
                userRepository.countOnline(now, now.minus(presenceProperties.onlineWindow())),
                userRepository.countBlocked(now));
    }

    private UserPresence presenceOf(final User user, final Instant now) {
        if (blockReasonOf(user, now) != null) {
            return UserPresence.BLOCKED;
        }
        Instant lastSeen = user.getLastSeenAt();
        boolean online = lastSeen != null && !lastSeen.isBefore(now.minus(presenceProperties.onlineWindow()));
        return online ? UserPresence.ONLINE : UserPresence.OFFLINE;
    }

    private BlockReason blockReasonOf(final User user, final Instant now) {
        if (!user.isActive()) {
            return BlockReason.SUSPENDED;
        }
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil != null && lockedUntil.isAfter(now) ? BlockReason.LOCKED : null;
    }

    /**
     * Suspends a user: marks the account inactive and revokes all refresh tokens.
     * Idempotent — suspending an already suspended user succeeds.
     *
     * @param userId  target user
     * @param actorId global admin performing the action
     * @throws EntityNotFoundException   if the user does not exist
     * @throws SelfDeactivationException if the target is the actor or the global-admin account
     */
    @Transactional
    public void suspend(final UUID userId, final UUID actorId) {
        User user = loadProtectedTarget(userId, actorId);
        refreshTokenRepository.deleteAllByUserId(userId);
        if (!user.isActive()) {
            return;
        }
        user.setActive(false);
        userRepository.save(user);
        record(EVENT_USER_DEACTIVATED, actorId, userId);
    }

    /**
     * Reactivates a suspended user. Idempotent.
     *
     * @param userId  target user
     * @param actorId global admin performing the action
     * @throws EntityNotFoundException if the user does not exist
     */
    @Transactional
    public void reactivate(final UUID userId, final UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (user.isActive()) {
            return;
        }
        user.setActive(true);
        userRepository.save(user);
        record(EVENT_USER_REACTIVATED, actorId, userId);
    }

    /**
     * Permanently deletes a user that has no related records.
     *
     * @param userId  target user
     * @param actorId global admin performing the action
     * @throws EntityNotFoundException   if the user does not exist
     * @throws SelfDeactivationException if the target is the actor or the global-admin account
     * @throws UserHasHistoryException   if other records still reference the user
     */
    @Transactional
    public void delete(final UUID userId, final UUID actorId) {
        User user = loadProtectedTarget(userId, actorId);
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            LOG.info("event=USER_DELETE_BLOCKED actorId={} targetUserId={}", actorId, userId);
            throw new UserHasHistoryException();
        }
        record(EVENT_USER_DELETED, actorId, userId);
    }

    private User loadProtectedTarget(final UUID userId, final UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (userId.equals(actorId) || globalAdminProperties.matches(user.getEmail())) {
            throw new SelfDeactivationException();
        }
        return user;
    }

    private void record(final String event, final UUID actorId, final UUID targetId) {
        auditLogService.record(new AuditLog.Builder(event)
                .userId(actorId)
                .outcome(OUTCOME_SUCCESS)
                .detail("targetUserId=" + targetId)
                .build());
        LOG.info("event={} actorId={} targetUserId={}", event, actorId, targetId);
    }
}
