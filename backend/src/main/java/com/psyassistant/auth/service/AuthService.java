package com.psyassistant.auth.service;

import com.psyassistant.auth.domain.RefreshToken;
import com.psyassistant.auth.domain.RefreshTokenRepository;
import com.psyassistant.auth.dto.FirstLoginPasswordChangeDto;
import com.psyassistant.auth.dto.LoginRequest;
import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityProperties;
import com.psyassistant.notifications.EmailMessage;
import com.psyassistant.notifications.EmailNotificationPort;
import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles authentication, token refresh, and logout.
 *
 * <p>Passwords are never logged. Emails are logged as SHA-256 hashes on failure.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    private static final String EVENT_LOGIN_FAILURE = "LOGIN_FAILURE";
    private static final String EVENT_LOGOUT = "LOGOUT";
    private static final String EVENT_TOKEN_REFRESH = "TOKEN_REFRESH";
    private static final String EVENT_REPLAY_ATTACK = "REPLAY_ATTACK";
    private static final String EVENT_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    private static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_FAILURE = "FAILURE";
    private static final String SHA_256 = "SHA-256";

    @Value("${app.security.session.max.admin}")
    private int maxAdminSessions;

    @Value("${app.security.session.max.user}")
    private int maxUserSessions;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final TherapistProfileRepository therapistProfileRepository;
    private final SecurityProperties securityProperties;
    private final EmailNotificationPort emailNotificationPort;

    /**
     * Constructs the service with all required collaborators.
     *
     * @param userRepository             user data access
     * @param refreshTokenRepository     refresh token data access
     * @param tokenService               JWT and token utilities
     * @param passwordEncoder            BCrypt encoder
     * @param auditLogService            audit recorder
     * @param therapistProfileRepository therapist profile repository
     * @param securityProperties         security policy settings
     * @param emailNotificationPort      outbound email queue port
     */
    public AuthService(
            final UserRepository userRepository,
            final RefreshTokenRepository refreshTokenRepository,
            final TokenService tokenService,
            final PasswordEncoder passwordEncoder,
            final AuditLogService auditLogService,
            final TherapistProfileRepository therapistProfileRepository,
            final SecurityProperties securityProperties,
            final EmailNotificationPort emailNotificationPort) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.therapistProfileRepository = therapistProfileRepository;
        this.securityProperties = securityProperties;
        this.emailNotificationPort = emailNotificationPort;
    }

    /**
     * Authenticates a user and issues an access token + raw refresh token.
     *
     * <p>On each failed attempt the failure counter is incremented atomically.
     * After {@code lockout.maxAttempts} consecutive failures the account is locked
     * for {@code lockout.durationMinutes} minutes and a notification email is queued.
     * A locked account returns HTTP 401 with {@code ACCOUNT_LOCKED} even if the
     * correct password is supplied.  The counter resets on a successful login.
     *
     * @param request   login credentials
     * @param ipAddress caller's IP address for audit
     * @return login response containing the access token
     * @throws AuthException with INVALID_CREDENTIALS, ACCOUNT_DISABLED, or ACCOUNT_LOCKED
     */
    @Transactional
    public AuthResult authenticate(final LoginRequest request, final String ipAddress) {
        String requestId = MDC.get("requestId");

        User user = userRepository.findByEmail(request.email()).orElse(null);

        // Always run BCrypt to prevent timing-based user-enumeration attacks.
        // The dummy hash is a valid BCrypt hash so the encoder does not throw.
        // It was generated from a random string and will never match any real password.
        String hashToCheck = (user != null) ? user.getPasswordHash()
                : "$2a$12$n9Rg0LXkEBWvl2rBYFPMCe0RjH1iExbRKIpSA3CqMy7KsBqDNLfIi";
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        // Lockout check: after BCrypt (for timing) but before credential validation
        if (user != null && user.isCurrentlyLocked()) {
            LOG.info("event=LOGIN_FAILURE userId={} code=ACCOUNT_LOCKED requestId={}",
                    user.getId(), requestId);
            recordFailure(EVENT_LOGIN_FAILURE, user.getId().toString(),
                    request.email(), ipAddress, requestId, "ACCOUNT_LOCKED");
            throw new AuthException(AuthException.ErrorCode.ACCOUNT_LOCKED,
                    "Account locked until " + user.getLockedUntil());
        }

        if (user == null || !passwordMatches) {
            if (user != null) {
                handleFailedLoginAttempt(user, request.email(), ipAddress, requestId);
            } else {
                recordFailure(EVENT_LOGIN_FAILURE, null, request.email(),
                        ipAddress, requestId, "INVALID_CREDENTIALS");
                LOG.info("event=LOGIN_FAILURE emailHash={} code=INVALID_CREDENTIALS requestId={}",
                        hashEmail(request.email()), requestId);
            }
            throw new AuthException(AuthException.ErrorCode.INVALID_CREDENTIALS,
                    "Invalid credentials");
        }

        if (!user.isActive()) {
            recordFailure(EVENT_LOGIN_FAILURE, user.getId().toString(),
                    request.email(), ipAddress, requestId, "ACCOUNT_DISABLED");
            LOG.info("event=LOGIN_FAILURE emailHash={} code=ACCOUNT_DISABLED requestId={}",
                    hashEmail(request.email()), requestId);
            throw new AuthException(AuthException.ErrorCode.ACCOUNT_DISABLED,
                    "Account disabled");
        }

        // Successful login: reset failure counter
        userRepository.clearFailedLoginAttempts(user.getId());

        enforceSessionCap(user);

        String rawToken = tokenService.generateRawRefreshToken();
        String hash = tokenService.hashRefreshToken(rawToken);
        Instant expiresAt = Instant.now().plus(tokenService.refreshTtlFor(user.getRole()));
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));

        // For therapists, look up their profile ID to include in the JWT
        UUID therapistProfileId = null;
        if (user.getRole() == UserRole.THERAPIST) {
            therapistProfileId = therapistProfileRepository.findByEmailIgnoreCase(user.getEmail())
                    .map(profile -> profile.getId())
                    .orElse(null);
            if (therapistProfileId != null) {
                LOG.debug("Including therapistProfileId={} in JWT for user={}",
                        therapistProfileId, user.getId());
            } else {
                LOG.warn("User userId={} has THERAPIST role but no TherapistProfile found",
                        user.getId());
            }
        }

        String accessToken = tokenService.buildAccessToken(user, therapistProfileId);
        Instant expiresAtAccess = tokenService.accessTokenExpiresAt(user.getRole());

        auditLogService.record(new AuditLog.Builder(EVENT_LOGIN_SUCCESS)
                .userId(user.getId())
                .emailAttempted(request.email())
                .ipAddress(ipAddress)
                .requestId(requestId)
                .outcome(OUTCOME_SUCCESS)
                .build());

        LOG.info("event=LOGIN_SUCCESS userId={} requestId={}", user.getId(), requestId);

        LoginResponse response = new LoginResponse(accessToken, expiresAtAccess, "Bearer");
        return new AuthResult(response, rawToken, user.getRole(),
                tokenService.refreshTtlFor(user.getRole()));
    }

    /**
     * Rotates a refresh token: validates the old one, deletes it, issues a new pair.
     *
     * @param rawToken  the raw refresh token from the cookie
     * @param ipAddress caller's IP address
     * @return new auth result
     * @throws AuthException with TOKEN_EXPIRED if the token is unknown or expired
     */
    @Transactional
    public AuthResult refresh(final String rawToken, final String ipAddress) {
        String requestId = MDC.get("requestId");
        String hash = tokenService.hashRefreshToken(rawToken);

        RefreshToken stored = refreshTokenRepository.findActiveByHash(hash).orElse(null);

        if (stored == null) {
            // Possible replay attack — nuke all user sessions if we can match the hash
            handlePossibleReplayAttack(hash, ipAddress, requestId);
            throw new AuthException(AuthException.ErrorCode.TOKEN_EXPIRED,
                    "Refresh token not found or expired");
        }

        User user = stored.getUser();

        // Guard: reject refresh for deactivated accounts
        if (!user.isActive()) {
            refreshTokenRepository.delete(stored);
            refreshTokenRepository.flush();
            recordFailure(EVENT_TOKEN_REFRESH, user.getId().toString(),
                    user.getEmail(), ipAddress, requestId, "ACCOUNT_DISABLED");
            LOG.info("event=TOKEN_REFRESH_DENIED userId={} reason=ACCOUNT_DISABLED requestId={}",
                    user.getId(), requestId);
            throw new AuthException(AuthException.ErrorCode.TOKEN_EXPIRED,
                    "Account disabled");
        }

        // Rotate: delete old, insert new
        refreshTokenRepository.delete(stored);
        refreshTokenRepository.flush();

        String newRawToken = tokenService.generateRawRefreshToken();
        String newHash = tokenService.hashRefreshToken(newRawToken);
        Instant newExpiry = Instant.now().plus(tokenService.refreshTtlFor(user.getRole()));
        refreshTokenRepository.save(new RefreshToken(user, newHash, newExpiry));

        // For therapists, look up their profile ID to include in the JWT
        UUID therapistProfileId = null;
        if (user.getRole() == UserRole.THERAPIST) {
            therapistProfileId = therapistProfileRepository.findByEmailIgnoreCase(user.getEmail())
                    .map(profile -> profile.getId())
                    .orElse(null);
        }

        String accessToken = tokenService.buildAccessToken(user, therapistProfileId);
        Instant accessExpiry = tokenService.accessTokenExpiresAt(user.getRole());

        auditLogService.record(new AuditLog.Builder(EVENT_TOKEN_REFRESH)
                .userId(user.getId())
                .ipAddress(ipAddress)
                .requestId(requestId)
                .outcome(OUTCOME_SUCCESS)
                .build());

        LOG.info("event=TOKEN_REFRESH userId={} requestId={}", user.getId(), requestId);

        LoginResponse response = new LoginResponse(accessToken, accessExpiry, "Bearer");
        return new AuthResult(response, newRawToken, user.getRole(),
                tokenService.refreshTtlFor(user.getRole()));
    }

    /**
     * Revokes the refresh token associated with the given raw cookie value.
     *
     * @param rawToken  raw refresh token from the cookie (may be null)
     * @param ipAddress caller's IP address
     */
    @Transactional
    public void logout(final String rawToken, final String ipAddress) {
        String requestId = MDC.get("requestId");
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String hash = tokenService.hashRefreshToken(rawToken);
        refreshTokenRepository.findActiveByHash(hash).ifPresent(token -> {
            User user = token.getUser();
            refreshTokenRepository.delete(token);
            auditLogService.record(new AuditLog.Builder(EVENT_LOGOUT)
                    .userId(user.getId())
                    .ipAddress(ipAddress)
                    .requestId(requestId)
                    .outcome(OUTCOME_SUCCESS)
                    .build());
            LOG.info("event=LOGOUT userId={} requestId={}", user.getId(), requestId);
        });
    }

    /**
     * Changes a user's password during first login when mustChangePassword is true.
     *
     * <p>Validates the current temporary password, sets the new permanent password,
     * clears the mustChangePassword flag, and issues new authentication tokens.
     *
     * @param userId    the user's UUID
     * @param dto       password change request
     * @param ipAddress caller's IP address
     * @return authentication result with new tokens
     * @throws AuthException if current password is invalid or user not found
     */
    @Transactional
    public AuthResult changePasswordFirstLogin(
            final UUID userId,
            final FirstLoginPasswordChangeDto dto,
            final String ipAddress) {
        String requestId = MDC.get("requestId");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthException.ErrorCode.INVALID_CREDENTIALS,
                        "User not found"));

        // Validate current password
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            recordFailure(EVENT_LOGIN_FAILURE, user.getId().toString(),
                    user.getEmail(), ipAddress, requestId, "Invalid current password");
            LOG.warn("event=PASSWORD_CHANGE_FAILURE userId={} requestId={} reason=InvalidCurrentPassword",
                    user.getId(), requestId);
            throw new AuthException(AuthException.ErrorCode.INVALID_CREDENTIALS,
                    "Current password is incorrect");
        }

        // Check if password change is actually required
        if (!user.isMustChangePassword()) {
            LOG.warn("event=PASSWORD_CHANGE_ATTEMPT userId={} requestId={} reason=NotRequired",
                    user.getId(), requestId);
        }

        // Update password using the User entity method
        user.updatePasswordHash(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        // Revoke all existing sessions
        refreshTokenRepository.deleteAllByUserId(user.getId());

        // Issue new authentication tokens
        String rawToken = tokenService.generateRawRefreshToken();
        String hash = tokenService.hashRefreshToken(rawToken);
        Instant expiresAt = Instant.now().plus(tokenService.refreshTtlFor(user.getRole()));
        RefreshToken refreshToken = new RefreshToken(user, hash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        // For therapists, look up their profile ID to include in the JWT
        UUID therapistProfileId = null;
        if (user.getRole() == UserRole.THERAPIST) {
            therapistProfileId = therapistProfileRepository.findByEmailIgnoreCase(user.getEmail())
                    .map(profile -> profile.getId())
                    .orElse(null);
        }

        String accessToken = tokenService.buildAccessToken(user, therapistProfileId);
        Instant accessExpiry = tokenService.accessTokenExpiresAt(user.getRole());

        auditLogService.record(new AuditLog.Builder(EVENT_PASSWORD_CHANGED)
                .userId(user.getId())
                .ipAddress(ipAddress)
                .requestId(requestId)
                .outcome(OUTCOME_SUCCESS)
                .detail("firstLogin=true")
                .build());

        LOG.info("event=PASSWORD_CHANGED userId={} requestId={} firstLogin=true",
                user.getId(), requestId);

        LoginResponse response = new LoginResponse(accessToken, accessExpiry, "Bearer");
        return new AuthResult(response, rawToken, user.getRole(),
                tokenService.refreshTtlFor(user.getRole()));
    }

    // ---- private helpers -----------------------------------------------

    /**
     * Handles a failed login attempt for a known user: increments the counter and,
     * if the threshold is reached, locks the account and queues a notification email.
     */
    private void handleFailedLoginAttempt(
            final User user, final String email,
            final String ipAddress, final String requestId) {
        userRepository.incrementFailedLoginAttempts(user.getId());
        int newCount = user.getFailedLoginAttempts() + 1;
        int maxAttempts = securityProperties.lockout().maxAttempts();

        recordFailure(EVENT_LOGIN_FAILURE, user.getId().toString(),
                email, ipAddress, requestId, "INVALID_CREDENTIALS attempt=" + newCount);
        LOG.info("event=LOGIN_FAILURE userId={} failedAttempts={} code=INVALID_CREDENTIALS requestId={}",
                user.getId(), newCount, requestId);

        if (newCount >= maxAttempts) {
            Instant lockUntil = Instant.now()
                    .plus(Duration.ofMinutes(securityProperties.lockout().durationMinutes()));
            userRepository.lockAccount(user.getId(), lockUntil);

            auditLogService.record(new AuditLog.Builder(EVENT_ACCOUNT_LOCKED)
                    .userId(user.getId())
                    .emailAttempted(email)
                    .ipAddress(ipAddress)
                    .requestId(requestId)
                    .outcome(OUTCOME_FAILURE)
                    .detail("lockedUntil=" + lockUntil)
                    .build());
            LOG.info("event=ACCOUNT_LOCKED userId={} lockedUntil={} requestId={}",
                    user.getId(), lockUntil, requestId);

            try {
                emailNotificationPort.queue(new EmailMessage(
                        email,
                        NotificationEventType.ACCOUNT_LOCKED,
                        "email.subject.account.locked",
                        "Your account has been locked until " + lockUntil
                                + " due to too many failed login attempts."));
            } catch (Exception ex) {
                LOG.warn("event=LOCKOUT_EMAIL_FAILED userId={} reason={}",
                        user.getId(), ex.getMessage());
            }
        }
    }

    private void enforceSessionCap(final User user) {
        boolean isAdminRole = user.getRole() == UserRole.SYSTEM_ADMINISTRATOR
                || user.getRole() == UserRole.ADMIN;
        int cap = isAdminRole ? maxAdminSessions : maxUserSessions;
        long active = refreshTokenRepository.countActiveSessions(user.getId());

        if (active >= cap) {
            if (isAdminRole) {
                // Admin cap=1: delete all existing sessions
                refreshTokenRepository.deleteAllByUserId(user.getId());
            } else {
                // Other roles cap=5: evict the oldest session
                List<RefreshToken> oldest =
                        refreshTokenRepository.findOldestActiveByUserId(user.getId());
                if (!oldest.isEmpty()) {
                    refreshTokenRepository.delete(oldest.get(0));
                }
            }
        }
    }

    private void handlePossibleReplayAttack(
            final String hash, final String ipAddress, final String requestId) {
        // We cannot identify the user from the unknown hash, so just log it
        auditLogService.record(new AuditLog.Builder(EVENT_REPLAY_ATTACK)
                .ipAddress(ipAddress)
                .requestId(requestId)
                .outcome(OUTCOME_FAILURE)
                .detail("Unknown hash=" + hash.substring(0, 8) + "...")
                .build());
        LOG.warn("event=REPLAY_ATTACK hashPrefix={} requestId={}",
                hash.substring(0, 8), requestId);
    }

    private void recordFailure(
            final String event, final String userId, final String email,
            final String ipAddress, final String requestId, final String detail) {
        auditLogService.record(new AuditLog.Builder(event)
                .userId(userId != null ? UUID.fromString(userId) : null)
                .emailAttempted(email)
                .ipAddress(ipAddress)
                .requestId(requestId)
                .outcome(OUTCOME_FAILURE)
                .detail(detail)
                .build());
    }

    private String hashEmail(final String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return "hash-unavailable";
        }
    }
}

