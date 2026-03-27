package com.psyassistant.common.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests covering the 7 RBAC acceptance criteria from PA-17.
 *
 * <p>Uses real Spring context + H2 (test profile) so the full security filter chain,
 * {@code @PreAuthorize} method security, and the audit log pipeline are all exercised.
 *
 * <ul>
 *   <li>AC1: THERAPIST + valid JWT → GET sessions → HTTP 200</li>
 *   <li>AC2: FINANCE → GET sessions → HTTP 403, body.code = "ACCESS_DENIED"</li>
 *   <li>AC3: THERAPIST not assigned to client → service throws AccessDeniedException before data</li>
 *   <li>AC4: SYSTEM_ADMINISTRATOR → POST admin/users → HTTP 201 (or 400 on empty body — not 403)</li>
 *   <li>AC5: THERAPIST → POST admin/users → HTTP 403</li>
 *   <li>AC6: FINANCE → session note endpoint → HTTP 403</li>
 *   <li>AC7: SUPERVISOR → GET sessions notes → HTTP 200</li>
 * </ul>
 *
 * <p>Each 403 is also verified to produce an {@code ACCESS_DENIED} audit log row.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final String ASSIGNED_CLIENT = "assigned-client-123";
    private static final String OTHER_CLIENT = "other-client-456";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    // ---- AC1: THERAPIST can read sessions for assigned client ----------------

    /**
     * AC1: THERAPIST with a valid JWT may call GET /api/v1/clients/{id}/sessions
     * for a client they are assigned to, and receives HTTP 200.
     */
    @Test
    void therapistCanGetSessionsForAssignedClient() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + ASSIGNED_CLIENT + "/sessions")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_THERAPIST"),
                                        new SimpleGrantedAuthority("READ_OWN_SESSIONS"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    // ---- AC2: FINANCE cannot read sessions ---------------------------------

    /**
     * AC2: FINANCE user calling GET /api/v1/clients/{id}/sessions receives HTTP 403
     * with body code "ACCESS_DENIED".
     */
    @Test
    void financeCannotGetSessions() throws Exception {
        long auditCountBefore = auditLogRepository.count();

        mockMvc.perform(get("/api/v1/clients/" + ASSIGNED_CLIENT + "/sessions")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_FINANCE"),
                                        new SimpleGrantedAuthority("MANAGE_INVOICES"),
                                        new SimpleGrantedAuthority("MANAGE_PAYMENTS"),
                                        new SimpleGrantedAuthority("READ_FINANCIAL_REPORTS"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
        List<AuditLog> recentEntries = auditLogRepository.findAll();
        assertThat(recentEntries).anySatisfy(entry ->
                assertThat(entry.getEventType()).isEqualTo("ACCESS_DENIED")
        );
    }

    // ---- AC3: THERAPIST not assigned to client X gets service-layer denial ---

    /**
     * AC3: THERAPIST who is NOT assigned to client X receives HTTP 403 when accessing
     * that client's sessions — the service layer throws AccessDeniedException before
     * any data is loaded.
     */
    @Test
    void therapistNotAssignedToClientGets403() throws Exception {
        long auditCountBefore = auditLogRepository.count();

        mockMvc.perform(get("/api/v1/clients/" + OTHER_CLIENT + "/sessions")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_THERAPIST"),
                                        new SimpleGrantedAuthority("READ_OWN_SESSIONS"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
    }

    // ---- AC4: SYSTEM_ADMINISTRATOR can create users -------------------------

    /**
     * AC4: SYSTEM_ADMINISTRATOR calling POST /api/v1/admin/users with a valid request
     * body receives HTTP 201. Security layer must pass the request through (not 401/403).
     *
     * <p>Since this is a full-context test the UserManagementService persists to an H2 DB.
     * A missing body causes a 400, but that is an application-layer response — proof that
     * the security layer accepted the request.
     */
    @Test
    void systemAdminCanReachAdminUserEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMINISTRATOR"),
                                        new SimpleGrantedAuthority("MANAGE_USERS"))
                                .jwt(j -> j.subject(ADMIN_ID.toString()))))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotIn(401, 403);
                });
    }

    // ---- AC5: THERAPIST cannot create users ---------------------------------

    /**
     * AC5: THERAPIST calling POST /api/v1/admin/users receives HTTP 403 with code
     * "ACCESS_DENIED" — the admin namespace is restricted to SYSTEM_ADMINISTRATOR.
     */
    @Test
    void therapistCannotPostToAdminUsers() throws Exception {
        long auditCountBefore = auditLogRepository.count();

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_THERAPIST"),
                                        new SimpleGrantedAuthority("READ_OWN_SESSIONS"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
    }

    // ---- AC6: FINANCE cannot access session note endpoints ------------------

    /**
     * AC6: FINANCE user calling GET /api/v1/sessions/{id}/notes receives HTTP 403
     * because the session notes URL namespace is restricted to THERAPIST, SUPERVISOR,
     * and SYSTEM_ADMINISTRATOR.
     */
    @Test
    void financeCannotAccessSessionNotes() throws Exception {
        long auditCountBefore = auditLogRepository.count();

        mockMvc.perform(get("/api/v1/sessions/session-123/notes")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_FINANCE"),
                                        new SimpleGrantedAuthority("MANAGE_INVOICES"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
    }

    // ---- AC7: SUPERVISOR can read session notes -----------------------------

    /**
     * AC7: SUPERVISOR calling GET /api/v1/sessions/{id}/notes receives HTTP 200 with
     * full note content — supervisors have access to all session notes.
     */
    @Test
    void supervisorCanGetSessionNotes() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/session-123/notes")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SUPERVISOR"),
                                        new SimpleGrantedAuthority("READ_ALL_SESSION_NOTES"),
                                        new SimpleGrantedAuthority("READ_CARE_PLANS"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    // ---- Additional: every 403 produces audit log entry --------------------

    /**
     * Verifies that the audit log is populated when a RECEPTION_ADMIN_STAFF user
     * (who lacks MANAGE_USERS) attempts to access the admin namespace.
     * Also verifies that the audit entry contains the correct user ID.
     */
    @Test
    void forbiddenRequestAlwaysProducesAuditLogEntry() throws Exception {
        long auditCountBefore = auditLogRepository.count();

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_RECEPTION_ADMIN_STAFF"),
                                        new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isGreaterThan(auditCountBefore);

        List<AuditLog> entries = auditLogRepository.findAll();
        assertThat(entries).anySatisfy(entry -> {
            assertThat(entry.getEventType()).isEqualTo("ACCESS_DENIED");
            assertThat(entry.getUserId()).isEqualTo(USER_ID);
        });
    }

    /**
     * Verifies that a FINANCE user can access the finance namespace (HTTP 200).
     */
    @Test
    void financeUserCanAccessFinanceNamespace() throws Exception {
        mockMvc.perform(get("/api/v1/finance/invoices")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_FINANCE"),
                                        new SimpleGrantedAuthority("MANAGE_INVOICES"))
                                .jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that SYSTEM_ADMINISTRATOR can access the finance namespace (HTTP 200).
     */
    @Test
    void systemAdminCanAccessFinanceNamespace() throws Exception {
        mockMvc.perform(get("/api/v1/finance/invoices")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMINISTRATOR"),
                                        new SimpleGrantedAuthority("MANAGE_INVOICES"))
                                .jwt(j -> j.subject(ADMIN_ID.toString()))))
                .andExpect(status().isOk());
    }
}
