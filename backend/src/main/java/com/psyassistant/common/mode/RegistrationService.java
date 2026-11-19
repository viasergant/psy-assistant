package com.psyassistant.common.mode;

import com.psyassistant.auth.service.AuthResult;
import com.psyassistant.auth.service.AuthService;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.organization.OrganizationStatus;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles self-service registration of new solo therapist organizations.
 *
 * <p>Each registration creates one {@link Organization} (type SOLO) and one
 * {@link User} with all five roles. Tokens are issued immediately so the caller
 * is logged in without a separate login step.
 *
 * <p>Email uniqueness is enforced globally: one email address may only register
 * once across all organizations, because the login flow identifies users by email.
 */
@Service
@Profile("saas")
public class RegistrationService {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    private static final Set<UserRole> OWNER_ROLES = EnumSet.of(
            UserRole.RECEPTION_ADMIN_STAFF,
            UserRole.THERAPIST,
            UserRole.SUPERVISOR,
            UserRole.FINANCE,
            UserRole.SYSTEM_ADMINISTRATOR
    );

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public RegistrationService(final OrganizationRepository organizationRepository,
                               final UserRepository userRepository,
                               final PasswordEncoder passwordEncoder,
                               final AuthService authService,
                               final AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    /**
     * Registers a new solo therapist organization and immediately issues tokens.
     *
     * @param request validated registration data
     * @return an {@link AuthResult} containing the access token and refresh token
     * @throws RegistrationException if the email is already registered
     */
    @Transactional
    public AuthResult register(final RegistrationRequest request) {
        if (userRepository.existsByEmail(request.ownerEmail())) {
            throw new RegistrationException("Email address is already registered");
        }

        String slug = generateUniqueSlug(request.orgName());
        Organization org = new Organization(
                request.orgName(), slug, OrganizationType.SOLO, OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(org);

        String hash = passwordEncoder.encode(request.password());
        User user = new User(request.ownerEmail(), hash, OWNER_ROLES, true);
        user.setOrgId(savedOrg.getId());
        User savedUser = userRepository.save(user);

        LOG.info("event=ORG_REGISTERED orgId={} userId={}", savedOrg.getId(), savedUser.getId());

        try {
            auditLogService.record(new AuditLog.Builder("ORG_REGISTRATION")
                    .userId(savedUser.getId())
                    .outcome("SUCCESS")
                    .detail("orgId=" + savedOrg.getId() + " slug=" + slug)
                    .build());
        } catch (Exception ex) {
            LOG.warn("Audit log failed: {}", ex.getMessage());
        }

        return authService.issueTokensForUser(savedUser);
    }

    private String generateUniqueSlug(final String orgName) {
        String base = orgName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() > 90) {
            base = base.substring(0, 90);
        }
        if (base.isBlank()) {
            base = "org";
        }

        String slug = base;
        int suffix = 2;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }
}
