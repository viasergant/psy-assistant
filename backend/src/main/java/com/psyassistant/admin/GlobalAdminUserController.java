package com.psyassistant.admin;

import com.psyassistant.users.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global administrator endpoints for viewing users across all organizations.
 *
 * <p>Requires the {@code GLOBAL_ADMIN} authority. Available in every deployment mode.
 */
@RestController
@RequestMapping("/api/v1/global-admin/users")
@PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
@Tag(name = "Global Admin — Users", description = "Read-only list of all registered users")
public class GlobalAdminUserController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "email", "fullName", "active");

    private final GlobalAdminUserService service;

    /**
     * Constructs the controller.
     *
     * @param service global admin user service
     */
    public GlobalAdminUserController(final GlobalAdminUserService service) {
        this.service = service;
    }

    /**
     * Returns a paginated, optionally filtered list of all users.
     *
     * @param role   optional role filter
     * @param active optional active-status filter
     * @param search optional email/full-name substring
     * @param page   zero-based page index
     * @param size   page size (clamped to 1–100)
     * @param sort   sort field and direction, e.g. {@code createdAt,desc}
     * @return 200 with a {@link GlobalUserPageResponse}
     */
    @Operation(summary = "List all users", description = "Users of every organization with org info")
    @ApiResponse(responseCode = "200", description = "User page",
            content = @Content(schema = @Schema(implementation = GlobalUserPageResponse.class)))
    @GetMapping
    public ResponseEntity<GlobalUserPageResponse> listUsers(
            @RequestParam(required = false) final UserRole role,
            @RequestParam(required = false) final Boolean active,
            @RequestParam(required = false) final String search,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt,desc") final String sort) {
        return ResponseEntity.ok(service.listUsers(role, active, search, buildPageable(page, size, sort)));
    }

    private Pageable buildPageable(final int page, final int size, final String sort) {
        int safePage = Math.max(0, page);
        int clampedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            return PageRequest.of(safePage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(Sort.Direction.DESC)
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, clampedSize, Sort.by(direction, field));
    }
}
