package com.psyassistant.crm.leads;

import com.psyassistant.common.exception.ErrorResponse;
import com.psyassistant.crm.leads.dto.CreateLeadRequest;
import com.psyassistant.crm.leads.dto.LeadDetailDto;
import com.psyassistant.crm.leads.dto.LeadPageResponse;
import com.psyassistant.crm.leads.dto.TransitionStatusRequest;
import com.psyassistant.crm.leads.dto.UpdateLeadRequest;
import com.psyassistant.users.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * REST endpoints for managing CRM lead profiles.
 *
 * <p>Write operations (create, update, archive) require the {@code MANAGE_LEADS} authority.
 * Read operations require either {@code MANAGE_LEADS} or {@code READ_LEADS}.
 */
@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "CRM — Lead Management",
        description = "Create, view, edit, and manage the lifecycle of lead profiles")
public class LeadController {

    private final LeadService leadService;

    /** Maximum allowed page size to prevent loading excessive data in one request. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Constructs the controller.
     *
     * @param leadService lead business logic service
     */
    public LeadController(final LeadService leadService) {
        this.leadService = leadService;
    }

    // ---- list leads -------------------------------------------------------

    /**
     * Returns a paginated, optionally filtered list of leads.
     *
     * @param status          optional status filter
     * @param ownerId         optional owner UUID filter
     * @param includeArchived when true, INACTIVE leads are included (default false)
     * @param page            zero-based page index (default 0)
     * @param size            page size (default 20, max 100)
     * @param sort            sort field and direction, e.g. {@code createdAt,desc}
     * @return 200 with a {@link LeadPageResponse}
     */
    @Operation(summary = "List leads",
            description = "Paginated list with optional status, owner, and archive filters")
    @ApiResponse(responseCode = "200", description = "Lead page",
            content = @Content(schema = @Schema(implementation = LeadPageResponse.class)))
    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_LEADS') or hasAuthority('READ_LEADS')")
    public ResponseEntity<LeadPageResponse> listLeads(
            @RequestParam(required = false) final LeadStatus status,
            @RequestParam(required = false) final UUID ownerId,
            @RequestParam(defaultValue = "false") final boolean includeArchived,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt,desc") final String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(leadService.listLeads(status, ownerId, includeArchived, pageable));
    }

    // ---- get single lead --------------------------------------------------

    /**
     * Returns the full detail view of a single lead.
     *
     * @param id the lead's UUID
     * @return 200 with a {@link LeadDetailDto}, or 404 if not found
     */
    @Operation(summary = "Get lead", description = "Returns the full detail view of a single lead")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lead detail",
            content = @Content(schema = @Schema(implementation = LeadDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Lead not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_LEADS') or hasAuthority('READ_LEADS')")
    public ResponseEntity<LeadDetailDto> getLead(@PathVariable final UUID id) {
        return ResponseEntity.ok(leadService.getLead(id));
    }

    // ---- create lead ------------------------------------------------------

    /**
     * Creates a new lead with at least one contact method.
     *
     * @param request validated creation payload
     * @return 201 with the created {@link LeadDetailDto}
     */
    @Operation(summary = "Create lead", description = "Creates a new lead profile")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lead created",
            content = @Content(schema = @Schema(implementation = LeadDetailDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_LEADS')")
    public ResponseEntity<LeadDetailDto> createLead(
            @Valid @RequestBody final CreateLeadRequest request) {
        UUID actorId = UserManagementService.currentPrincipalId();
        LeadDetailDto created = leadService.createLead(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ---- update lead ------------------------------------------------------

    /**
     * Fully replaces an existing lead's data.
     *
     * @param id      the lead's UUID
     * @param request validated update payload
     * @return 200 with the updated {@link LeadDetailDto}
     */
    @Operation(summary = "Update lead", description = "Full replacement of lead profile data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lead updated",
            content = @Content(schema = @Schema(implementation = LeadDetailDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Lead not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_LEADS')")
    public ResponseEntity<LeadDetailDto> updateLead(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateLeadRequest request) {
        UUID actorId = UserManagementService.currentPrincipalId();
        return ResponseEntity.ok(leadService.updateLead(id, request, actorId));
    }

    // ---- transition status ------------------------------------------------

    /**
     * Transitions a lead to a new status (FSM enforced).
     *
     * @param id      the lead's UUID
     * @param request the desired target status
     * @return 200 with the updated {@link LeadDetailDto}, or 422 on invalid transition
     */
    @Operation(summary = "Transition lead status",
            description = "Moves the lead through its lifecycle FSM")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status transitioned",
            content = @Content(schema = @Schema(implementation = LeadDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Lead not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Invalid status transition",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_LEADS')")
    public ResponseEntity<LeadDetailDto> transitionStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final TransitionStatusRequest request) {
        UUID actorId = UserManagementService.currentPrincipalId();
        return ResponseEntity.ok(leadService.transitionStatus(id, request.status(), actorId));
    }

    // ---- archive ----------------------------------------------------------

    /**
     * Archives a lead by transitioning it to INACTIVE.
     *
     * @param id the lead's UUID
     * @return 200 with the archived {@link LeadDetailDto}, or 422 if already terminal
     */
    @Operation(summary = "Archive lead",
            description = "Transitions the lead to INACTIVE (terminal archive status)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lead archived",
            content = @Content(schema = @Schema(implementation = LeadDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Lead not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Lead already in terminal state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('MANAGE_LEADS')")
    public ResponseEntity<LeadDetailDto> archiveLead(@PathVariable final UUID id) {
        UUID actorId = UserManagementService.currentPrincipalId();
        return ResponseEntity.ok(leadService.archiveLead(id, actorId));
    }

    // ---- helpers ----------------------------------------------------------

    /**
     * Parses a {@code field,direction} sort string into a {@link Pageable}.
     * Page index is clamped to zero-or-above; size is clamped to [1, {@value MAX_PAGE_SIZE}].
     */
    private Pageable buildPageable(final int page, final int size, final String sort) {
        int safePage = Math.max(0, page);
        int clampedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction direction = parts.length > 1
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.DESC;
            return PageRequest.of(safePage, clampedSize, Sort.by(direction, field));
        } catch (Exception ex) {
            return PageRequest.of(safePage, clampedSize,
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}
