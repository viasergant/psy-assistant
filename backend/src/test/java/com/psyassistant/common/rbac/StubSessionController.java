package com.psyassistant.common.rbac;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal stub controller used by RBAC integration tests.
 *
 * <p>Simulates the session and session-note endpoints that will be implemented
 * in future milestones. Registered only in the {@code test} profile.
 */
@Profile("test")
@RestController
@RequestMapping("/api/v1")
public class StubSessionController {

    private static final String ASSIGNED_CLIENT_ID = "assigned-client-123";

    /**
     * Simulates GET /api/v1/clients/{clientId}/sessions.
     *
     * <p>Service-layer enforcement (Layer 2):
     * <ul>
     *   <li>Only users with {@code READ_OWN_SESSIONS} or {@code READ_ALL_SESSIONS}
     *       permission may access this endpoint (THERAPIST and SUPERVISOR/SYSTEM_ADMINISTRATOR).</li>
     *   <li>THERAPIST (READ_OWN_SESSIONS only) is further restricted to assigned clients only.</li>
     * </ul>
     *
     * @param clientId client UUID path variable
     * @param authentication current security principal
     * @return 200 OK with empty list placeholder
     */
    @PreAuthorize("hasAuthority('READ_OWN_SESSIONS') or hasAuthority('READ_ALL_SESSIONS')")
    @GetMapping("/clients/{clientId}/sessions")
    public ResponseEntity<String> getSessions(
            @PathVariable final String clientId,
            final Authentication authentication) {

        // Service-layer ownership guard: THERAPIST may only access assigned clients
        boolean canReadAll = authentication.getAuthorities().stream()
                .anyMatch(a -> "READ_ALL_SESSIONS".equals(a.getAuthority()));

        if (!canReadAll && !ASSIGNED_CLIENT_ID.equals(clientId)) {
            // Throw before any data is loaded — satisfies AC3
            throw new AccessDeniedException(
                    "Therapist is not assigned to client: " + clientId);
        }

        return ResponseEntity.ok("[]");
    }

    /**
     * Simulates GET /api/v1/sessions/{sessionId}/notes.
     *
     * <p>Security enforced at URL layer: only THERAPIST, SUPERVISOR, SYSTEM_ADMINISTRATOR
     * may reach this endpoint (configured in {@code SecurityConfig}).
     *
     * @param sessionId session UUID path variable
     * @return 200 OK with note placeholder
     */
    @GetMapping("/sessions/{sessionId}/notes")
    public ResponseEntity<String> getSessionNotes(@PathVariable final String sessionId) {
        return ResponseEntity.ok("{\"note\": \"session note content\"}");
    }

    /**
     * Simulates GET /api/v1/finance/invoices — finance namespace endpoint.
     *
     * <p>Security enforced at URL layer: only FINANCE or SYSTEM_ADMINISTRATOR may reach this.
     *
     * @return 200 OK
     */
    @GetMapping("/finance/invoices")
    public ResponseEntity<String> getInvoices() {
        return ResponseEntity.ok("[]");
    }
}
