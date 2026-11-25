package com.psyassistant.common.mode;

import com.psyassistant.auth.token.OneTimeTokenRepository;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes registrations whose email address was never confirmed within the retention period,
 * together with their tokens and (when empty) their organization.
 *
 * <p>Unverified users cannot log in, so no business data references them.
 */
@Component
@Profile("saas")
public class UnverifiedRegistrationCleanupJob {

    static final Duration RETENTION = Duration.ofDays(7);

    private static final Logger LOG = LoggerFactory.getLogger(UnverifiedRegistrationCleanupJob.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OneTimeTokenRepository tokenRepository;
    private final Clock clock;

    /**
     * Production constructor.
     *
     * @param userRepository         user data access
     * @param organizationRepository organization data access
     * @param tokenRepository        one-time token data access
     */
    @Autowired
    public UnverifiedRegistrationCleanupJob(final UserRepository userRepository,
                                            final OrganizationRepository organizationRepository,
                                            final OneTimeTokenRepository tokenRepository) {
        this(userRepository, organizationRepository, tokenRepository, Clock.systemUTC());
    }

    UnverifiedRegistrationCleanupJob(final UserRepository userRepository,
                                     final OrganizationRepository organizationRepository,
                                     final OneTimeTokenRepository tokenRepository,
                                     final Clock clock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }

    /**
     * Runs daily and purges stale unverified registrations.
     *
     * @return number of users removed
     */
    @Scheduled(cron = "${app.registration.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public int purgeStale() {
        Instant cutoff = clock.instant().minus(RETENTION);
        List<User> stale = userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(cutoff);
        if (stale.isEmpty()) {
            return 0;
        }
        tokenRepository.deleteAllByUserIdIn(stale.stream().map(User::getId).toList());
        for (User user : stale) {
            UUID orgId = user.getOrgId();
            userRepository.delete(user);
            userRepository.flush();
            if (userRepository.countByOrgId(orgId) == 0) {
                organizationRepository.deleteById(orgId);
            }
        }
        LOG.info("event=UNVERIFIED_REGISTRATIONS_PURGED count={}", stale.size());
        return stale.size();
    }
}
