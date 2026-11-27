package com.psyassistant.auth.service;

import com.psyassistant.users.UserRepository;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Rejects access tokens of users that were suspended or deleted after the token was issued,
 * so that suspension takes effect immediately instead of after the token expires.
 *
 * <p>Performs a single primary-key lookup of the {@code active} flag per request and records the
 * user's activity (throttled) for the presence marker.
 */
@Component
public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ACCOUNT_DISABLED =
            new OAuth2Error("invalid_token", "Account is disabled or no longer exists", null);

    private final UserRepository userRepository;
    private final UserActivityTracker activityTracker;

    /**
     * Constructs the validator.
     *
     * @param userRepository  user repository
     * @param activityTracker records the last activity of accepted users
     */
    public ActiveUserJwtValidator(final UserRepository userRepository, final UserActivityTracker activityTracker) {
        this.userRepository = userRepository;
        this.activityTracker = activityTracker;
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return OAuth2TokenValidatorResult.failure(ACCOUNT_DISABLED);
        }
        boolean active = userRepository.findActiveById(userId).orElse(false);
        if (!active) {
            return OAuth2TokenValidatorResult.failure(ACCOUNT_DISABLED);
        }
        activityTracker.recordActivity(userId);
        return OAuth2TokenValidatorResult.success();
    }
}
