package com.psyassistant.billing.catalog.rest;

import com.psyassistant.billing.catalog.ServiceCatalogService;
import com.psyassistant.billing.catalog.ServiceStatus;
import com.psyassistant.billing.catalog.dto.CreateServiceRequest;
import com.psyassistant.billing.catalog.dto.PriceHistoryResponse;
import com.psyassistant.billing.catalog.dto.ServiceCatalogListItem;
import com.psyassistant.billing.catalog.dto.ServiceCatalogResponse;
import com.psyassistant.billing.catalog.dto.TherapistOverrideResponse;
import com.psyassistant.billing.catalog.dto.UpdateDefaultPriceRequest;
import com.psyassistant.billing.catalog.dto.UpdateServiceRequest;
import com.psyassistant.billing.catalog.dto.UpdateServiceStatusRequest;
import com.psyassistant.billing.catalog.dto.UpsertTherapistOverrideRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the service catalog feature.
 *
 * <p>Base path: {@code /api/v1/billing/catalog}
 * <p>Authorization is enforced at the service layer via {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/billing/catalog")
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    public ServiceCatalogController(final ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceCatalogListItem>> list(
            @RequestParam(required = false) final ServiceStatus status) {
        return ResponseEntity.ok(catalogService.listServices(status));
    }

    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> create(
            @Valid @RequestBody final CreateServiceRequest request) {
        ServiceCatalogResponse created = catalogService.createService(request);
        URI location = URI.create("/api/v1/billing/catalog/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> get(@PathVariable final UUID id) {
        return ResponseEntity.ok(catalogService.getService(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> update(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateServiceRequest request) {
        return ResponseEntity.ok(catalogService.updateService(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceCatalogResponse> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateServiceStatusRequest request) {
        return ResponseEntity.ok(catalogService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ServiceCatalogResponse> updatePrice(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateDefaultPriceRequest request) {
        return ResponseEntity.ok(catalogService.updateDefaultPrice(id, request));
    }

    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<PriceHistoryResponse>> priceHistory(@PathVariable final UUID id) {
        return ResponseEntity.ok(catalogService.getPriceHistory(id));
    }

    @GetMapping("/{id}/overrides")
    public ResponseEntity<List<TherapistOverrideResponse>> listOverrides(@PathVariable final UUID id) {
        return ResponseEntity.ok(catalogService.getTherapistOverrides(id));
    }

    @PutMapping("/{id}/overrides/{therapistId}")
    public ResponseEntity<TherapistOverrideResponse> upsertOverride(
            @PathVariable final UUID id,
            @PathVariable final UUID therapistId,
            @Valid @RequestBody final UpsertTherapistOverrideRequest request) {
        return ResponseEntity.ok(catalogService.upsertTherapistOverride(id, therapistId, request));
    }

    @DeleteMapping("/{id}/overrides/{therapistId}")
    public ResponseEntity<Void> deleteOverride(
            @PathVariable final UUID id,
            @PathVariable final UUID therapistId) {
        catalogService.deleteTherapistOverride(id, therapistId);
        return ResponseEntity.noContent().build();
    }
}
