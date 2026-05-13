package com.psyassistant.riskflags.rest;

import com.psyassistant.riskflags.dto.CreateRiskFlagTypeRequest;
import com.psyassistant.riskflags.dto.RiskFlagTypeResponse;
import com.psyassistant.riskflags.service.RiskFlagTypeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for risk flag type configuration.
 *
 * <p>Two base paths:
 * <ul>
 *   <li>{@code GET /api/v1/risk-flag-types} — public read (all authenticated staff)</li>
 *   <li>{@code GET /api/v1/admin/risk-flag-types} — admin: list all including inactive</li>
 *   <li>{@code POST /api/v1/admin/risk-flag-types} — admin: create a new flag type</li>
 *   <li>{@code PATCH /api/v1/admin/risk-flag-types/{id}/deactivate} — admin: soft-delete</li>
 * </ul>
 */
@RestController
public class RiskFlagTypeController {

    private static final Logger LOG = LoggerFactory.getLogger(RiskFlagTypeController.class);

    private final RiskFlagTypeService riskFlagTypeService;

    public RiskFlagTypeController(final RiskFlagTypeService riskFlagTypeService) {
        this.riskFlagTypeService = riskFlagTypeService;
    }

    /**
     * Returns all active risk flag types ordered by display order.
     * Available to all authenticated staff — used to populate the create-flag dropdown.
     *
     * @return 200 with list of active flag types
     */
    @GetMapping("/api/v1/risk-flag-types")
    public ResponseEntity<List<RiskFlagTypeResponse>> listActive() {
        return ResponseEntity.ok(riskFlagTypeService.listActive());
    }

    /**
     * Returns all risk flag types including inactive ones.
     * Requires {@code MANAGE_RISK_FLAG_TYPES}.
     *
     * @return 200 with all flag types ordered by display order
     */
    @GetMapping("/api/v1/admin/risk-flag-types")
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")
    public ResponseEntity<List<RiskFlagTypeResponse>> listAll() {
        return ResponseEntity.ok(riskFlagTypeService.listAll());
    }

    /**
     * Creates a new active risk flag type.
     * Requires {@code MANAGE_RISK_FLAG_TYPES}.
     *
     * @param request validated creation payload
     * @return 201 with the created flag type and a {@code Location} header
     */
    @PostMapping("/api/v1/admin/risk-flag-types")
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")
    public ResponseEntity<RiskFlagTypeResponse> create(
            @Valid @RequestBody final CreateRiskFlagTypeRequest request) {

        LOG.info("POST /api/v1/admin/risk-flag-types name={}", request.name());

        final RiskFlagTypeResponse created =
                riskFlagTypeService.create(request.name(), request.displayOrder());
        final URI location = URI.create(
                "/api/v1/admin/risk-flag-types/" + created.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location)
                .body(created);
    }

    /**
     * Soft-deletes a risk flag type so it no longer appears in the creation form.
     * Requires {@code MANAGE_RISK_FLAG_TYPES}.
     *
     * @param id UUID of the flag type to deactivate
     * @return 200 with the updated flag type
     */
    @PatchMapping("/api/v1/admin/risk-flag-types/{id}/deactivate")
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")
    public ResponseEntity<RiskFlagTypeResponse> deactivate(@PathVariable final UUID id) {
        LOG.info("PATCH /api/v1/admin/risk-flag-types/{}/deactivate", id);
        return ResponseEntity.ok(riskFlagTypeService.deactivate(id));
    }
}
