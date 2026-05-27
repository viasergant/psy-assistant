package com.psyassistant.billing.pkg.rest;

import com.psyassistant.billing.pkg.PackageService;
import com.psyassistant.billing.pkg.dto.PackageInstanceResponse;
import com.psyassistant.billing.pkg.dto.SellPackageRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for selling prepaid packages to clients.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code POST /api/v1/billing/packages/sell} — sell a package instance to a client</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/billing/packages")
public class PackageController {

    private final PackageService packageService;

    public PackageController(final PackageService packageService) {
        this.packageService = packageService;
    }

    @PostMapping("/sell")
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    public ResponseEntity<PackageInstanceResponse> sell(
            @Valid @RequestBody final SellPackageRequest request,
            final Authentication auth) {

        PackageInstanceResponse response = packageService.sellPackage(request, auth.getName());
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/clients/{clientId}/packages/{id}")
                .buildAndExpand(response.clientId(), response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
