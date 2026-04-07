package com.psyassistant.careplans.rest;

import com.psyassistant.careplans.domain.CarePlanStatus;
import com.psyassistant.careplans.dto.CarePlanAuditResponse;
import com.psyassistant.careplans.dto.CarePlanDetailResponse;
import com.psyassistant.careplans.dto.CarePlanSummaryResponse;
import com.psyassistant.careplans.dto.CreateCarePlanRequest;
import com.psyassistant.careplans.dto.CreateGoalRequest;
import com.psyassistant.careplans.dto.CreateInterventionRequest;
import com.psyassistant.careplans.dto.CreateMilestoneRequest;
import com.psyassistant.careplans.dto.GoalResponse;
import com.psyassistant.careplans.dto.InterventionResponse;
import com.psyassistant.careplans.dto.MilestoneResponse;
import com.psyassistant.careplans.dto.UpdateCarePlanRequest;
import com.psyassistant.careplans.dto.UpdateGoalStatusRequest;
import com.psyassistant.careplans.service.CarePlanService;
import com.psyassistant.users.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for care plan management.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET /api/v1/clients/{clientId}/care-plans} — list plans for a client</li>
 *   <li>{@code POST /api/v1/clients/{clientId}/care-plans} — create</li>
 *   <li>{@code GET /api/v1/care-plans/{planId}} — detail</li>
 *   <li>{@code PUT /api/v1/care-plans/{planId}} — update header</li>
 *   <li>{@code POST /api/v1/care-plans/{planId}/close} — close</li>
 *   <li>{@code POST /api/v1/care-plans/{planId}/archive} — archive</li>
 *   <li>{@code GET /api/v1/care-plans/{planId}/audit} — audit log</li>
 *   <li>Goals, Interventions, Milestones under {@code /api/v1/care-plans/{planId}/goals/...}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class CarePlanController {

    private static final Logger LOG = LoggerFactory.getLogger(CarePlanController.class);

    private final CarePlanService carePlanService;
    private final UserRepository userRepository;

    public CarePlanController(final CarePlanService carePlanService,
                               final UserRepository userRepository) {
        this.carePlanService = carePlanService;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // Care Plans
    // =========================================================================

    @GetMapping("/clients/{clientId}/care-plans")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<List<CarePlanSummaryResponse>> listByClient(
            @PathVariable final UUID clientId,
            @RequestParam(required = false) final CarePlanStatus status,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(carePlanService.listByClient(clientId, status, actorId, hasReadAll));
    }

    @PostMapping("/clients/{clientId}/care-plans")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<CarePlanDetailResponse> create(
            @PathVariable final UUID clientId,
            @Valid @RequestBody final CreateCarePlanRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        LOG.info("POST /api/v1/clients/{}/care-plans by actor={}", clientId, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.create(clientId, request, actorId, actorName));
    }

    @GetMapping("/care-plans/{planId}")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<CarePlanDetailResponse> getDetail(
            @PathVariable final UUID planId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(carePlanService.getDetail(planId, actorId, hasReadAll));
    }

    @PutMapping("/care-plans/{planId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<CarePlanDetailResponse> update(
            @PathVariable final UUID planId,
            @Valid @RequestBody final UpdateCarePlanRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.ok(carePlanService.update(planId, request, actorId, actorName));
    }

    @PostMapping("/care-plans/{planId}/close")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable final UUID planId, final Authentication auth) {
        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        carePlanService.close(planId, actorId, actorName);
    }

    @PostMapping("/care-plans/{planId}/archive")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable final UUID planId, final Authentication auth) {
        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        carePlanService.archive(planId, actorId, actorName);
    }

    @GetMapping("/care-plans/{planId}/audit")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<Page<CarePlanAuditResponse>> getAuditLog(
            @PathVariable final UUID planId,
            @PageableDefault(size = 20, sort = "actionTimestamp") final Pageable pageable,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(carePlanService.getAuditLog(planId, actorId, hasReadAll, pageable));
    }

    // =========================================================================
    // Goals
    // =========================================================================

    @PostMapping("/care-plans/{planId}/goals")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<GoalResponse> addGoal(
            @PathVariable final UUID planId,
            @Valid @RequestBody final CreateGoalRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.addGoal(planId, request, actorId, actorName));
    }

    @PutMapping("/care-plans/{planId}/goals/{goalId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @Valid @RequestBody final CreateGoalRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.ok(carePlanService.updateGoal(planId, goalId, request, actorId, actorName));
    }

    @PatchMapping("/care-plans/{planId}/goals/{goalId}/status")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<GoalResponse> updateGoalStatus(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @Valid @RequestBody final UpdateGoalStatusRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.ok(carePlanService.updateGoalStatus(planId, goalId, request, actorId, actorName));
    }

    @DeleteMapping("/care-plans/{planId}/goals/{goalId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGoal(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        carePlanService.removeGoal(planId, goalId, actorId, actorName);
    }

    // =========================================================================
    // Interventions
    // =========================================================================

    @PostMapping("/care-plans/{planId}/goals/{goalId}/interventions")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<InterventionResponse> addIntervention(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @Valid @RequestBody final CreateInterventionRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.addIntervention(planId, goalId, request, actorId, actorName));
    }

    @PutMapping("/care-plans/{planId}/goals/{goalId}/interventions/{interventionId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<InterventionResponse> updateIntervention(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @PathVariable final UUID interventionId,
            @Valid @RequestBody final CreateInterventionRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.ok(
                carePlanService.updateIntervention(planId, goalId, interventionId, request, actorId, actorName));
    }

    @DeleteMapping("/care-plans/{planId}/goals/{goalId}/interventions/{interventionId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeIntervention(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @PathVariable final UUID interventionId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        carePlanService.removeIntervention(planId, goalId, interventionId, actorId, actorName);
    }

    // =========================================================================
    // Milestones
    // =========================================================================

    @PostMapping("/care-plans/{planId}/goals/{goalId}/milestones")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<MilestoneResponse> addMilestone(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @Valid @RequestBody final CreateMilestoneRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.addMilestone(planId, goalId, request, actorId, actorName));
    }

    @PutMapping("/care-plans/{planId}/goals/{goalId}/milestones/{milestoneId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<MilestoneResponse> updateMilestone(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @PathVariable final UUID milestoneId,
            @Valid @RequestBody final CreateMilestoneRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.ok(
                carePlanService.updateMilestone(planId, goalId, milestoneId, request, actorId, actorName));
    }

    @PostMapping("/care-plans/{planId}/goals/{goalId}/milestones/{milestoneId}/achieve")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<MilestoneResponse> achieveMilestone(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @PathVariable final UUID milestoneId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        return ResponseEntity.ok(
                carePlanService.achieveMilestone(planId, goalId, milestoneId, actorId, actorName));
    }

    @DeleteMapping("/care-plans/{planId}/goals/{goalId}/milestones/{milestoneId}")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMilestone(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @PathVariable final UUID milestoneId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        carePlanService.removeMilestone(planId, goalId, milestoneId, actorId, actorName);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private UUID actorId(final Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    private String resolveActorName(final UUID actorId, final Authentication auth) {
        return userRepository.findById(actorId)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse(auth.getName());
    }

    private boolean hasAuthority(final Authentication auth, final String authority) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
