package com.psyassistant.therapists.rest;

import com.psyassistant.therapists.dto.CreatePricingRuleRequest;
import com.psyassistant.therapists.dto.PricingRuleResponse;
import com.psyassistant.therapists.service.TherapistPricingRuleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for therapist session-type pricing rules.
 *
 * <p>Provides endpoints to list and create pricing rules scoped to a therapist profile.
 * The session type is specified by its UUID (from {@code GET /api/v1/appointments/session-types}).
 */
@RestController
@RequestMapping("/api/v1/therapists/{therapistId}/pricing-rules")
public class TherapistPricingRuleController {

    private final TherapistPricingRuleService pricingRuleService;

    public TherapistPricingRuleController(final TherapistPricingRuleService pricingRuleService) {
        this.pricingRuleService = pricingRuleService;
    }

    /**
     * GET /api/v1/therapists/{therapistId}/pricing-rules
     *
     * <p>Returns all pricing rules for the given therapist, newest first.
     *
     * @param therapistId therapist profile UUID
     * @return 200 OK with list of pricing rules, or 404 if therapist not found
     */
    @GetMapping
    public ResponseEntity<List<PricingRuleResponse>> listPricingRules(
            @PathVariable final UUID therapistId) {
        try {
            return ResponseEntity.ok(pricingRuleService.listRules(therapistId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/v1/therapists/{therapistId}/pricing-rules
     *
     * <p>Creates a new pricing rule for the therapist identified by {@code therapistId}.
     * The request body must include a valid active {@code sessionTypeId}.
     *
     * @param therapistId therapist profile UUID
     * @param request     the create request
     * @param principal   the authenticated user principal (for audit)
     * @return 201 Created with the new rule, 400 for validation errors, 404 if not found
     */
    @PostMapping
    public ResponseEntity<PricingRuleResponse> createPricingRule(
            @PathVariable final UUID therapistId,
            @Valid @RequestBody final CreatePricingRuleRequest request,
            final Principal principal) {
        try {
            String actorName = principal != null ? principal.getName() : "system";
            PricingRuleResponse response = pricingRuleService.createRule(therapistId, request, actorName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
