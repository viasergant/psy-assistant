package com.psyassistant.reporting.caseload;

import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates caseload read operations with supervisor-scoped access enforcement.
 *
 * <p><strong>Scope rules:</strong>
 * <ul>
 *     <li>SYSTEM_ADMINISTRATOR — sees all active therapists</li>
 *     <li>SUPERVISOR — sees only therapists in their {@code supervisor_team_member} rows</li>
 * </ul>
 *
 * <p>All page load events are written to the audit log at the top of each method.
 */
@Service
public class CaseloadService {

    private static final Logger LOG = LoggerFactory.getLogger(CaseloadService.class);
    private static final String CASELOAD_VIEWED = "CASELOAD_VIEWED";
    private static final String CASELOAD_DRILLDOWN_VIEWED = "CASELOAD_DRILLDOWN_VIEWED";
    private static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMINISTRATOR";

    private final CaseloadSnapshotRepository snapshotRepository;
    private final SupervisorTeamRepository teamRepository;
    private final CaseloadQueryService queryService;
    private final AuditLogService auditLogService;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param snapshotRepository repository for caseload snapshot reads
     * @param teamRepository     repository for supervisor team membership
     * @param queryService       native-SQL drill-down query service
     * @param auditLogService    audit log writer
     */
    public CaseloadService(
            final CaseloadSnapshotRepository snapshotRepository,
            final SupervisorTeamRepository teamRepository,
            final CaseloadQueryService queryService,
            final AuditLogService auditLogService) {
        this.snapshotRepository = snapshotRepository;
        this.teamRepository = teamRepository;
        this.queryService = queryService;
        this.auditLogService = auditLogService;
    }

    /**
     * Returns a paginated caseload overview for the requesting user.
     *
     * <p>SYSTEM_ADMINISTRATOR sees all active therapists; SUPERVISOR sees only
     * therapists in their team.
     *
     * @param requestingUserId  UUID of the authenticated user
     * @param isAdmin           true if the user has SYSTEM_ADMINISTRATOR role
     * @param specializationIds optional specialization filter (null = no filter)
     * @param snapshotDate      specific snapshot date (null = latest available)
     * @param pageable          pagination parameters
     * @return page of caseload rows
     */
    @Transactional(readOnly = true)
    public Page<CaseloadRowResponse> getCaseload(
            final UUID requestingUserId,
            final boolean isAdmin,
            final List<UUID> specializationIds,
            final LocalDate snapshotDate,
            final Pageable pageable) {

        recordAudit(CASELOAD_VIEWED, requestingUserId, null);

        final Set<UUID> allowedTherapistIds = resolveAllowedTherapistIds(requestingUserId, isAdmin);
        if (allowedTherapistIds.isEmpty()) {
            LOG.debug("getCaseload: empty team for userId={}", requestingUserId);
            return Page.empty(pageable);
        }

        final Page<CaseloadSnapshot> snapshots = snapshotRepository.findLatestByTherapistIds(
                allowedTherapistIds, specializationIds, snapshotDate, pageable);

        final Map<UUID, String> nameMap = buildNameMap(snapshots);

        final List<CaseloadRowResponse> rows = snapshots.getContent().stream()
                .map(s -> toResponse(s, nameMap))
                .toList();

        return new PageImpl<>(rows, pageable, snapshots.getTotalElements());
    }

    /**
     * Returns a paginated client list for the specified therapist.
     *
     * <p>Supervisors may only drill down into therapists in their own team.
     * System Administrators may access any therapist.
     *
     * @param requestingUserId   UUID of the authenticated user
     * @param isAdmin            true if the user has SYSTEM_ADMINISTRATOR role
     * @param therapistProfileId UUID of the therapist to drill into
     * @param pageable           pagination parameters
     * @return page of client rows (no clinical data)
     * @throws AccessDeniedException if supervisor tries to access a therapist outside their team
     */
    @Transactional(readOnly = true)
    public Page<TherapistClientRow> getTherapistClients(
            final UUID requestingUserId,
            final boolean isAdmin,
            final UUID therapistProfileId,
            final Pageable pageable) {

        recordAudit(CASELOAD_DRILLDOWN_VIEWED, requestingUserId, therapistProfileId.toString());

        if (!isAdmin) {
            final Set<UUID> teamIds = teamRepository.findTherapistIdsBySupervisorId(requestingUserId);
            if (!teamIds.contains(therapistProfileId)) {
                throw new AccessDeniedException(
                        "Supervisor does not have access to therapist " + therapistProfileId);
            }
        }

        return queryService.getClientList(therapistProfileId, pageable);
    }

    // ---- private helpers -----------------------------------------------

    private Set<UUID> resolveAllowedTherapistIds(final UUID userId, final boolean isAdmin) {
        if (isAdmin) {
            return snapshotRepository.findAllActiveTherapistProfileIds();
        }
        return teamRepository.findTherapistIdsBySupervisorId(userId);
    }

    private Map<UUID, String> buildNameMap(final Page<CaseloadSnapshot> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        final Set<UUID> ids = new java.util.HashSet<>();
        page.forEach(s -> ids.add(s.getTherapistProfileId()));
        final List<Object[]> rows = snapshotRepository.findTherapistNamesById(ids);
        final Map<UUID, String> map = new HashMap<>();
        for (Object[] row : rows) {
            final UUID id = row[0] instanceof UUID u ? u : UUID.fromString(row[0].toString());
            map.put(id, row[1] != null ? row[1].toString() : "");
        }
        return map;
    }

    private CaseloadRowResponse toResponse(
            final CaseloadSnapshot snapshot,
            final Map<UUID, String> nameMap) {
        return new CaseloadRowResponse(
                snapshot.getTherapistProfileId(),
                nameMap.getOrDefault(snapshot.getTherapistProfileId(), ""),
                snapshot.getActiveClientCount(),
                snapshot.getSessionsThisWeek(),
                snapshot.getSessionsThisMonth(),
                snapshot.getScheduledHoursThisWeek(),
                snapshot.getContractedHoursPerWeek(),
                snapshot.getUtilizationRate()
        );
    }

    private void recordAudit(final String event, final UUID userId, final String detail) {
        try {
            auditLogService.record(new AuditLog.Builder(event)
                    .userId(userId)
                    .outcome("SUCCESS")
                    .detail(detail)
                    .build());
        } catch (Exception ex) {
            LOG.warn("Failed to record audit event={}: {}", event, ex.getMessage());
        }
    }
}
