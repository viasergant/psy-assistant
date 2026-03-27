package com.psyassistant.admin;

import com.psyassistant.common.exception.ErrorResponse;
import com.psyassistant.users.UserManagementService;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.dto.CreateUserRequest;
import com.psyassistant.users.dto.PatchUserRequest;
import com.psyassistant.users.dto.UserPageResponse;
import com.psyassistant.users.dto.UserSummaryDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST endpoints for managing internal user accounts.
 *
 * <p>All methods require the {@code ROLE_SYSTEM_ADMINISTRATOR} authority.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
@Tag(name = "Admin — User Management",
        description = "Create, list, edit role/status, and reset passwords for internal users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    /**
     * Constructs the controller with its required service.
     *
     * @param userManagementService user management service
     */
    public AdminUserController(final UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Returns a paginated, optionally filtered list of user accounts.
     *
     * @param role   optional role filter
     * @param active optional active-status filter
     * @param page   zero-based page index (default 0)
     * @param size   page size (default 20)
     * @param sort   sort field and direction, e.g. {@code createdAt,desc} (default)
     * @return 200 with a {@link UserPageResponse}
     */
    @Operation(summary = "List users",
            description = "Paginated list with optional role and status filters")
    @ApiResponse(responseCode = "200", description = "User page",
            content = @Content(schema = @Schema(implementation = UserPageResponse.class)))
    @GetMapping
    public ResponseEntity<UserPageResponse> listUsers(
            @RequestParam(required = false) final UserRole role,
            @RequestParam(required = false) final Boolean active,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt,desc") final String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(userManagementService.listUsers(role, active, pageable));
    }

    /**
     * Creates a new user account (no initial password; a reset link is issued automatically).
     *
     * @param request validated creation payload
     * @return 201 with the created {@link UserSummaryDto}
     */
    @Operation(summary = "Create user",
            description = "Creates an account in Active state and issues a 24h password setup link")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(schema = @Schema(implementation = UserSummaryDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UserSummaryDto> createUser(
            @Valid @RequestBody final CreateUserRequest request) {
        UUID actorId = UserManagementService.currentPrincipalId();
        UserSummaryDto created = userManagementService.createUser(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Partially updates a user (full name, role, and/or active status).
     *
     * @param id      target user UUID
     * @param request fields to change (all optional)
     * @return 200 with the updated {@link UserSummaryDto}
     */
    @Operation(summary = "Update user",
            description = "Partial update: role, full name, or active status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated",
            content = @Content(schema = @Schema(implementation = UserSummaryDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or self-deactivation",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<UserSummaryDto> updateUser(
            @PathVariable final UUID id,
            @Valid @RequestBody final PatchUserRequest request) {
        UUID actorId = UserManagementService.currentPrincipalId();
        return ResponseEntity.ok(userManagementService.updateUser(id, request, actorId));
    }

    /**
     * Initiates an admin-driven password reset for the specified user.
     *
     * <p>Issues a 24-hour single-use reset link. The raw token is included in the
     * response body so the caller can send it via email (email delivery is out of
     * scope for this endpoint).
     *
     * @param id target user UUID
     * @return 204 No Content (the raw token is logged server-side; not returned to avoid leaking)
     */
    @Operation(summary = "Initiate password reset",
            description = "Generates a 24h single-use reset link for the target user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reset token issued"),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/password-reset")
    public ResponseEntity<Void> initiatePasswordReset(@PathVariable final UUID id) {
        UUID actorId = UserManagementService.currentPrincipalId();
        // Raw token is returned but intentionally not echoed in the response body —
        // the email service (external) would receive it via an event/queue in production.
        userManagementService.initiatePasswordReset(id, actorId);
        return ResponseEntity.noContent().build();
    }

    // ---- helpers -------------------------------------------------------

    /** Maximum allowed page size to prevent loading excessive data in one request. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Parses a {@code field,direction} sort string (e.g. {@code createdAt,desc}) into a
     * {@link Pageable}.  Page index is clamped to zero-or-above; page size is clamped to
     * [1, {@value MAX_PAGE_SIZE}].  Falls back to {@code createdAt DESC} on sort parse errors.
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
            return PageRequest.of(safePage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}
