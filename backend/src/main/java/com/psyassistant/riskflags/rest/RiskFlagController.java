package com.psyassistant.riskflags.rest;

import com.psyassistant.riskflags.dto.CreateRiskFlagRequest;
import com.psyassistant.riskflags.dto.ResolveRiskFlagRequest;
import com.psyassistant.riskflags.dto.RiskFlagResponse;
import com.psyassistant.riskflags.service.RiskFlagService;
import com.psyassistant.users.UserManagementService;
import com.psyassistant.users.UserRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for risk flag management on a client profile.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET  /api/v1/clients/{clientId}/risk-flags} — list active flags</li>
 *   <li>{@code GET  /api/v1/clients/{clientId}/risk-flags/history} — full flag history</li>
 *   <li>{@code POST /api/v1/clients/{clientId}/risk-flags} — raise a new flag</li>
 *   <li>{@code PATCH /api/v1/clients/{clientId}/risk-flags/{flagId}/resolve} — resolve a flag</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/risk-flags")
public class RiskFlagController {

    private static final Logger LOG = LoggerFactory.getLogger(RiskFlagController.class);

    private final RiskFlagService riskFlagService;
    private final UserRepository userRepository;

    public RiskFlagController(final RiskFlagService riskFlagService,
                               final UserRepository userRepository) {
        this.riskFlagService = riskFlagService;
        this.userRepository = userRepository;
    }

    /**
     * Returns all active risk flags for a client.
     * Clinical notes are stripped for callers lacking {@code READ_RISK_FLAG_NOTES}
     * (handled in the service layer).
     *
     * @param clientId client UUID
     * @return 200 with list of active flags
     */
    @GetMapping
    @PreAuthorize("hasAuthority('READ_RISK_FLAGS')")
    public ResponseEntity<List<RiskFlagResponse>> listActive(
            @PathVariable final UUID clientId) {
        return ResponseEntity.ok(riskFlagService.listActiveFlags(clientId));
    }

    /**
     * Returns the full risk flag history for a client (active and resolved).
     * Requires {@code READ_RISK_FLAG_NOTES} — available to THERAPIST, SUPERVISOR, and SYS_ADMIN.
     *
     * @param clientId client UUID
     * @return 200 with all flags newest first
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('READ_RISK_FLAG_NOTES')")
    public ResponseEntity<List<RiskFlagResponse>> listHistory(
            @PathVariable final UUID clientId) {
        return ResponseEntity.ok(riskFlagService.listAllFlags(clientId));
    }

    /**
     * Raises a new risk flag on the specified client profile.
     *
     * @param clientId client UUID
     * @param request  validated flag creation payload
     * @param auth     current authentication principal
     * @return 201 with the created flag and a {@code Location} header
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAGS')")
    public ResponseEntity<RiskFlagResponse> create(
            @PathVariable final UUID clientId,
            @Valid @RequestBody final CreateRiskFlagRequest request,
            final Authentication auth) {

        final UUID actorId = UserManagementService.currentPrincipalId();
        final String actorName = resolveActorName(actorId, auth);
        LOG.info("POST /api/v1/clients/{}/risk-flags by actor={}", clientId, actorId);

        final RiskFlagResponse created = riskFlagService.createFlag(clientId, request, actorId, actorName);
        final URI location = URI.create(
                "/api/v1/clients/" + clientId + "/risk-flags/" + created.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .body(created);
    }

    /**
     * Resolves an active risk flag.
     *
     * @param clientId client UUID (used to verify flag ownership)
     * @param flagId   UUID of the flag to resolve
     * @param request  validated resolution payload
     * @param auth     current authentication principal
     * @return 200 with the updated flag
     */
    @PatchMapping("/{flagId}/resolve")
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAGS')")
    public ResponseEntity<RiskFlagResponse> resolve(
            @PathVariable final UUID clientId,
            @PathVariable final UUID flagId,
            @Valid @RequestBody final ResolveRiskFlagRequest request,
            final Authentication auth) {

        final UUID actorId = UserManagementService.currentPrincipalId();
        final String actorName = resolveActorName(actorId, auth);
        LOG.info("PATCH /api/v1/clients/{}/risk-flags/{}/resolve by actor={}", clientId, flagId, actorId);

        return ResponseEntity.ok(
                riskFlagService.resolveFlag(clientId, flagId, request, actorId, actorName));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String resolveActorName(final UUID actorId, final Authentication auth) {
        return userRepository.findById(actorId)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse(auth.getName());
    }
}
