package com.psyassistant.common.mode;

import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.organization.OrganizationStatus;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One-time setup endpoint for clinic deployment mode.
 *
 * <p>Creates the initial {@code SYSTEM_ADMINISTRATOR} user when zero users exist.
 * Subsequent calls return HTTP 409 once at least one user is present, permanently
 * disabling the endpoint without requiring a server restart.
 *
 * <p>The endpoint is publicly accessible (no authentication) because it runs before
 * any user account exists. Spring Security is configured to {@code permitAll()} for
 * {@code /api/v1/setup/admin} in {@code SecurityConfig}.
 *
 * <p>This controller is absent in solo deployment mode because of the
 * {@link Profile @Profile("clinic")} annotation — Spring does not register it
 * and the path returns HTTP 404 when the {@code clinic} profile is not active.
 */
@RestController
@Profile("clinic")
@RequestMapping("/api/v1/setup")
public class ClinicSetupController {

    private static final Logger LOG = LoggerFactory.getLogger(ClinicSetupController.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param userRepository         JPA repository for {@link User} entities
     * @param organizationRepository JPA repository for {@link Organization} entities
     * @param passwordEncoder        Spring Security password encoder (BCrypt)
     * @param auditLogService        service for recording security-relevant events
     */
    public ClinicSetupController(final UserRepository userRepository,
                                 final OrganizationRepository organizationRepository,
                                 final PasswordEncoder passwordEncoder,
                                 final AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Creates the initial clinic admin user.
     *
     * <p>Returns HTTP 201 with a {@code Location} header and {@code {"id": "<uuid>"}} on success.
     * Returns HTTP 409 with {@code ErrorResponse} if any user already exists.
     * Returns HTTP 400 with {@code ErrorResponse} if validation fails.
     *
     * <p>The {@code count()} check and {@code save()} execute within a single transaction so
     * that two concurrent first-boot requests cannot both pass the zero-users gate. The
     * {@code email} column also carries a {@code UNIQUE} constraint (V2 migration) which acts
     * as a hard backstop against any race that slips through.
     *
     * @param request validated request body containing email, password, and optional full name
     * @return 201 response with {@code Location} header and body {@code {"id": "<uuid>"}}
     * @throws SetupAlreadyCompleteException if any user already exists in the database
     */
    @PostMapping("/admin")
    @Transactional
    public ResponseEntity<Map<String, String>> createAdmin(
            @Valid @RequestBody final ClinicSetupRequest request) {
        if (userRepository.count() > 0) {
            throw new SetupAlreadyCompleteException();
        }

        Organization org = organizationRepository.findAll().stream().findFirst()
                .orElseGet(() -> organizationRepository.save(
                        new Organization("Clinic", "clinic",
                                OrganizationType.CLINIC, OrganizationStatus.ACTIVE)));

        String hash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), hash, request.fullName(),
                Set.of(UserRole.SYSTEM_ADMINISTRATOR), true);
        user.setOrgId(org.getId());

        User saved = userRepository.save(user);
        LOG.info("event=CLINIC_ADMIN_CREATED userId={}", saved.getId());

        try {
            AuditLog entry = new AuditLog.Builder("CLINIC_ADMIN_CREATED")
                    .userId(saved.getId())
                    .outcome("SUCCESS")
                    .detail("Clinic mode initial admin user created via setup endpoint")
                    .build();
            auditLogService.record(entry);
        } catch (Exception ex) {
            LOG.warn("Audit log failed: {}", ex.getMessage());
        }

        URI location = URI.create("/api/v1/setup/admin/" + saved.getId());
        return ResponseEntity.created(location).body(Map.of("id", saved.getId().toString()));
    }
}
