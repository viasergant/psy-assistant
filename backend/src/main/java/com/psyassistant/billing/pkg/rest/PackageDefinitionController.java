package com.psyassistant.billing.pkg.rest;

import com.psyassistant.billing.pkg.PackageDefinitionStatus;
import com.psyassistant.billing.pkg.PackageService;
import com.psyassistant.billing.pkg.dto.CreatePackageDefinitionRequest;
import com.psyassistant.billing.pkg.dto.PackageDefinitionResponse;
import com.psyassistant.billing.pkg.dto.UpdatePackageDefinitionStatusRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for prepaid package definitions (the catalogue of sellable packages).
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET /api/v1/billing/package-definitions} — list, optionally filtered by status</li>
 *   <li>{@code POST /api/v1/billing/package-definitions} — create a new definition</li>
 *   <li>{@code PATCH /api/v1/billing/package-definitions/{id}/status} — archive or re-activate</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/billing/package-definitions")
public class PackageDefinitionController {

    private final PackageService packageService;

    public PackageDefinitionController(final PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PACKAGES')")
    public ResponseEntity<List<PackageDefinitionResponse>> list(
            @RequestParam(required = false) final PackageDefinitionStatus status) {
        return ResponseEntity.ok(packageService.listDefinitions(status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    public ResponseEntity<PackageDefinitionResponse> create(
            @Valid @RequestBody final CreatePackageDefinitionRequest request,
            final Authentication auth) {

        PackageDefinitionResponse response = packageService.createDefinition(request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    public ResponseEntity<PackageDefinitionResponse> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdatePackageDefinitionStatusRequest request,
            final Authentication auth) {

        return ResponseEntity.ok(packageService.updateDefinitionStatus(id, request, auth.getName()));
    }
}
