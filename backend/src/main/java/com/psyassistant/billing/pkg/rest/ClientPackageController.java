package com.psyassistant.billing.pkg.rest;

import com.psyassistant.billing.pkg.PackageService;
import com.psyassistant.billing.pkg.dto.PackageInstanceResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for accessing prepaid package instances scoped to a specific client.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET /api/v1/clients/{clientId}/packages} — list all package instances for a client</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/packages")
public class ClientPackageController {

    private final PackageService packageService;

    public ClientPackageController(final PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PACKAGES')")
    public ResponseEntity<List<PackageInstanceResponse>> listForClient(
            @PathVariable final UUID clientId) {
        return ResponseEntity.ok(packageService.listClientPackages(clientId));
    }
}
