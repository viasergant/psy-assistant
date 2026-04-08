package com.psyassistant.reporting.caseload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Therapist Caseload Overview feature (PA-30).
 *
 * <p>All endpoints require the {@code READ_TEAM_WORKLOAD} permission:
 * <ul>
 *     <li>SUPERVISOR — sees only therapists in their assigned team</li>
 *     <li>SYSTEM_ADMINISTRATOR — sees all therapists</li>
 *     <li>Any other role — receives HTTP 403</li>
 * </ul>
 *
 * <p>All access events are written to the audit log by {@link CaseloadService}.
 */
@RestController
@RequestMapping("/api/v1/caseload")
public class CaseloadController {

    private static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMINISTRATOR";

    private final CaseloadService caseloadService;

    /**
     * Constructs the controller with the required service.
     *
     * @param caseloadService caseload orchestration service
     */
    public CaseloadController(final CaseloadService caseloadService) {
        this.caseloadService = caseloadService;
    }

    /**
     * Returns a paginated caseload overview for all accessible therapists.
     *
     * <p>Optional filters: {@code specializationIds} (multi-value) and {@code snapshotDate}
     * (defaults to the most recent available snapshot).
     *
     * @param specializationIds optional filter by specialization UUIDs
     * @param snapshotDate      optional specific snapshot date (ISO 8601)
     * @param pageable          pagination (default page size 20)
     * @param authentication    JWT authentication from Spring Security
     * @return page of caseload rows
     */
    @GetMapping
    @PreAuthorize("hasAuthority('READ_TEAM_WORKLOAD')")
    public Page<CaseloadRowResponse> getCaseload(
            @RequestParam(required = false) final List<UUID> specializationIds,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate snapshotDate,
            @PageableDefault(size = 20) final Pageable pageable,
            final Authentication authentication) {

        final UUID userId = extractUserId(authentication);
        final boolean isAdmin = isSystemAdmin(authentication);
        return caseloadService.getCaseload(userId, isAdmin, specializationIds, snapshotDate, pageable);
    }

    /**
     * Returns a paginated client list for the specified therapist.
     *
     * <p>SUPERVISOR must have the therapist in their team; otherwise HTTP 403 is returned.
     * No clinical data fields are included in the response.
     *
     * @param therapistProfileId UUID of the therapist_profile record
     * @param pageable           pagination (default page size 20, max 50)
     * @param authentication     JWT authentication from Spring Security
     * @return page of client rows
     */
    @GetMapping("/{therapistProfileId}/clients")
    @PreAuthorize("hasAuthority('READ_TEAM_WORKLOAD')")
    public Page<TherapistClientRow> getTherapistClients(
            @PathVariable final UUID therapistProfileId,
            @PageableDefault(size = 20) final Pageable pageable,
            final Authentication authentication) {

        final UUID userId = extractUserId(authentication);
        final boolean isAdmin = isSystemAdmin(authentication);
        return caseloadService.getTherapistClients(userId, isAdmin, therapistProfileId, pageable);
    }

    // ---- private helpers -----------------------------------------------

    /**
     * Extracts the user UUID from the JWT subject claim.
     *
     * @param auth Spring Security authentication
     * @return user UUID or null if not parseable
     */
    private UUID extractUserId(final Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            final String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException ignored) {
                    // fall through to null
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the authenticated user has the SYSTEM_ADMINISTRATOR role.
     *
     * @param auth Spring Security authentication
     * @return true if system administrator
     */
    private boolean isSystemAdmin(final Authentication auth) {
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_SYSTEM_ADMIN.equals(a.getAuthority()));
    }
}
