package com.psyassistant.common.mode;

import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialises the single user account on first boot in solo deployment mode.
 *
 * <p>Reads {@code SOLO_USERNAME} (email) and {@code SOLO_PASSWORD} from environment variables
 * and creates a user with all five roles and {@code mustChangePassword = true}. The runner
 * is idempotent: if any user already exists in the database, it logs a skip event and returns
 * without creating a duplicate.
 *
 * <p>Plain-text passwords, usernames, and email addresses are never written to logs.
 */
@Component
@Profile("solo")
public class SoloUserInitRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SoloUserInitRunner.class);

    private static final Set<UserRole> ALL_ROLES = EnumSet.of(
        UserRole.RECEPTION_ADMIN_STAFF,
        UserRole.THERAPIST,
        UserRole.SUPERVISOR,
        UserRole.FINANCE,
        UserRole.SYSTEM_ADMINISTRATOR
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${SOLO_USERNAME:}")
    private String soloUsername;

    @Value("${SOLO_PASSWORD:}")
    private String soloPassword;

    /**
     * Constructs the runner with required dependencies.
     *
     * @param userRepository  JPA repository for {@link User} entities
     * @param passwordEncoder Spring Security password encoder (BCrypt)
     * @param auditLogService service for recording security-relevant events
     */
    public SoloUserInitRunner(final UserRepository userRepository,
                              final PasswordEncoder passwordEncoder,
                              final AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (userRepository.count() > 0) {
            LOG.info("event=SOLO_INIT_SKIPPED reason=users_exist");
            return;
        }

        if (soloUsername.isBlank() || soloPassword.isBlank()) {
            throw new IllegalStateException(
                "SOLO_USERNAME and SOLO_PASSWORD environment variables must be set"
            );
        }

        String hash = passwordEncoder.encode(soloPassword);
        User user = new User(soloUsername, hash, ALL_ROLES, true);
        user.setMustChangePassword(true);

        User saved = userRepository.save(user);
        LOG.info("event=SOLO_USER_CREATED userId={}", saved.getId());

        try {
            AuditLog entry = new AuditLog.Builder("SOLO_USER_CREATED")
                .userId(saved.getId())
                .outcome("SUCCESS")
                .detail("Solo mode initial user created on first boot")
                .build();
            auditLogService.record(entry);
        } catch (Exception ex) {
            LOG.warn("Audit log failed: {}", ex.getMessage());
        }
    }
}
