package com.psyassistant.billing.discount.rest;

import com.psyassistant.billing.discount.DiscountRuleService;
import com.psyassistant.billing.discount.dto.CreateDiscountRuleRequest;
import com.psyassistant.billing.discount.dto.DiscountRuleResponse;
import com.psyassistant.billing.discount.dto.UpdateDiscountRuleStatusRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
 * REST controller for discount rules.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET /api/v1/billing/discounts} — list all, or filter by clientId / serviceCatalogId</li>
 *   <li>{@code POST /api/v1/billing/discounts} — create a new rule</li>
 *   <li>{@code PATCH /api/v1/billing/discounts/{id}/status} — toggle active state</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/billing/discounts")
public class DiscountRuleController {

    private final DiscountRuleService service;

    public DiscountRuleController(final DiscountRuleService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DiscountRuleResponse>> list(
            @RequestParam(required = false) final UUID clientId,
            @RequestParam(required = false) final UUID serviceCatalogId) {
        return ResponseEntity.ok(service.list(clientId, serviceCatalogId));
    }

    @PostMapping
    public ResponseEntity<DiscountRuleResponse> create(
            @Valid @RequestBody final CreateDiscountRuleRequest request,
            final Authentication auth) {

        DiscountRuleResponse response = service.create(request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DiscountRuleResponse> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateDiscountRuleStatusRequest request,
            final Authentication auth) {

        return ResponseEntity.ok(service.toggleActive(id, request.active(), auth.getName()));
    }
}
