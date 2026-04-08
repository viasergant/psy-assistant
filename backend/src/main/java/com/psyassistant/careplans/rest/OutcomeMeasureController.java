package com.psyassistant.careplans.rest;

import com.psyassistant.careplans.dto.OutcomeMeasureChartDataResponse;
import com.psyassistant.careplans.dto.OutcomeMeasureDefinitionResponse;
import com.psyassistant.careplans.dto.OutcomeMeasureEntryResponse;
import com.psyassistant.careplans.dto.RecordOutcomeMeasureRequest;
import com.psyassistant.careplans.service.OutcomeMeasureService;
import com.psyassistant.users.UserRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for outcome measure definitions and entries.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET  /api/v1/outcome-measure-definitions}</li>
 *   <li>{@code POST /api/v1/care-plans/{planId}/outcome-measures}</li>
 *   <li>{@code GET  /api/v1/care-plans/{planId}/outcome-measures}</li>
 *   <li>{@code GET  /api/v1/care-plans/{planId}/outcome-measures/chart-data}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class OutcomeMeasureController {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeMeasureController.class);

    private final OutcomeMeasureService outcomeMeasureService;
    private final UserRepository userRepository;

    public OutcomeMeasureController(final OutcomeMeasureService outcomeMeasureService,
                                     final UserRepository userRepository) {
        this.outcomeMeasureService = outcomeMeasureService;
        this.userRepository = userRepository;
    }

    @GetMapping("/outcome-measure-definitions")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<List<OutcomeMeasureDefinitionResponse>> getDefinitions() {
        return ResponseEntity.ok(outcomeMeasureService.getDefinitions());
    }

    @PostMapping("/care-plans/{planId}/outcome-measures")
    @PreAuthorize("hasAuthority('MANAGE_CARE_PLANS')")
    public ResponseEntity<OutcomeMeasureEntryResponse> recordEntry(
            @PathVariable final UUID planId,
            @Valid @RequestBody final RecordOutcomeMeasureRequest request,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final String actorName = resolveActorName(actorId, auth);
        LOG.info("POST outcome-measure: planId={}, measure={}, actor={}",
                planId, request.measureDefinitionId(), actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(outcomeMeasureService.recordEntry(planId, request, actorId, actorName));
    }

    @GetMapping("/care-plans/{planId}/outcome-measures")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<Page<OutcomeMeasureEntryResponse>> getEntries(
            @PathVariable final UUID planId,
            @RequestParam(required = false) final String measureCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @PageableDefault(size = 20) final Pageable pageable,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(
                outcomeMeasureService.getEntries(planId, measureCode, from, to,
                        actorId, hasReadAll, pageable));
    }

    @GetMapping("/care-plans/{planId}/outcome-measures/chart-data")
    @PreAuthorize("hasAuthority('READ_CARE_PLANS')")
    public ResponseEntity<OutcomeMeasureChartDataResponse> getChartData(
            @PathVariable final UUID planId,
            @RequestParam final String measureCode,
            final Authentication auth) {

        final UUID actorId = actorId(auth);
        final boolean hasReadAll = hasAuthority(auth, "READ_CLIENTS_ALL");
        return ResponseEntity.ok(
                outcomeMeasureService.getChartData(planId, measureCode, actorId, hasReadAll));
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
