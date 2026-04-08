package com.psyassistant.careplans.rest;

import com.psyassistant.careplans.dto.CreateProgressNoteRequest;
import com.psyassistant.careplans.dto.GoalProgressHistoryResponse;
import com.psyassistant.careplans.dto.GoalProgressNoteResponse;
import com.psyassistant.careplans.service.GoalProgressService;
import com.psyassistant.users.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for goal progress notes and history.
 *
 * <p>URL structure: {@code /api/v1/care-plans/{planId}/goals/{goalId}/...}
 */
@RestController
@RequestMapping("/api/v1/care-plans/{planId}/goals/{goalId}")
public class GoalProgressController {

    private static final Logger LOG = LoggerFactory.getLogger(GoalProgressController.class);

    private final GoalProgressService goalProgressService;
    private final UserRepository userRepository;

    public GoalProgressController(final GoalProgressService goalProgressService,
                                   final UserRepository userRepository) {
        this.goalProgressService = goalProgressService;
        this.userRepository = userRepository;
    }

    @PostMapping("/progress-notes")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<GoalProgressNoteResponse> addProgressNote(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            @Valid @RequestBody final CreateProgressNoteRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        LOG.info("POST progress-note: planId={}, goalId={}, actor={}", planId, goalId, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalProgressService.addProgressNote(planId, goalId, request.noteText(),
                        actorId, actorName));
    }

    @GetMapping("/progress-notes")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<List<GoalProgressNoteResponse>> getProgressNotes(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(
                goalProgressService.getProgressNotes(planId, goalId, actorId, hasReadAll));
    }

    @GetMapping("/progress-history")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<GoalProgressHistoryResponse> getProgressHistory(
            @PathVariable final UUID planId,
            @PathVariable final UUID goalId,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(
                goalProgressService.getProgressHistory(planId, goalId, actorId, hasReadAll));
    }

    // ---- helpers ----

    private UUID actorId(final Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    private String resolveActorName(final UUID actorId, final Authentication auth) {
        return userRepository.findById(actorId)
                .map(u -> u.getFullName() != null ? u.getFullName() : auth.getName())
                .orElse(auth.getName());
    }

    private boolean hasAuthority(final Authentication auth, final String authority) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
