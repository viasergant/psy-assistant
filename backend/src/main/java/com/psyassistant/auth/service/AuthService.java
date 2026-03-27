package com.psyassistant.auth.service;

import com.psyassistant.auth.domain.RefreshToken;
import com.psyassistant.auth.domain.RefreshTokenRepository;
import com.psyassistant.auth.dto.LoginRequest;
import com.psyassistant.auth.dto.LoginResponse;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    /**
     * Constructs the service with all required collaborators.
     *
     * @param userRepository         user data access
     * @param refreshTokenRepository refresh token data access
     * @param tokenService           JWT and token utilities
     * @param passwordEncoder        BCrypt encoder
     * @param auditLogService        audit recorder
     */
    public AuthService(
            final UserRepository userRepository,
            final RefreshTokenRepository refreshTokenRepository,
            final TokenService tokenService,
            final PasswordEncoder passwordEncoder,
            final AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Authenticates a user and issues an access token + raw refresh token.
     *
     * @param request   login credentials
     * @param ipAddress caller's IP address for audit
     * @return login response containing the access token
     * @throws AuthException with INVALID_CREDENTIALS or ACCOUNT_DISABLED
     */
    @Transactional
    public AuthResult authenticate(final LoginRequest request, final String ipAddress) {
        String requestId = MDC.get("requestId");

        User user = userRepository.findByEmail(request.email()).orElse(null);

        // Always run BCrypt to prevent timing-based user-enumeration attacks.
        // Use a dummy hash when the user does not exist.
        String hashToCheck = (user != null) ? user.getPasswordHash()
                : "$2a$12$invalidhashpaddingtomatchbcryptlength00000000000000000000";
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            recordFailure(EVENT_LOGIN_FAILURE, null, request.email(),
                    ipAddress, requestId, "INVALID_CREDENTIALS");
            LOG.info("event=LOGIN_FAILURE emailHash={} code=INVALID_CREDENTIALS requestId={}",
                    hashEmail(request.email()), requestId);
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

        enforceSessionCap(user);

        String rawToken = tokenService.generateRawRefreshToken();
        String hash = tokenService.hashRefreshToken(rawToken);
        Instant expiresAt = Instant.now().plus(tokenService.refreshTtlFor(user.getRole()));
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));

        String accessToken = tokenService.buildAccessToken(user);
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

        // Rotate: delete old, insert new
        refreshTokenRepository.delete(stored);
        refreshTokenRepository.flush();

        String newRawToken = tokenService.generateRawRefreshToken();
        String newHash = tokenService.hashRefreshToken(newRawToken);
        Instant newExpiry = Instant.now().plus(tokenService.refreshTtlFor(user.getRole()));
        refreshTokenRepository.save(new RefreshToken(user, newHash, newExpiry));

        String accessToken = tokenService.buildAccessToken(user);
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

    // ---- private helpers -----------------------------------------------

    private void enforceSessionCap(final User user) {
        int cap = user.getRole() == UserRole.ADMIN ? maxAdminSessions : maxUserSessions;
        long active = refreshTokenRepository.countActiveSessions(user.getId());

        if (active >= cap) {
            if (user.getRole() == UserRole.ADMIN) {
                // Admin cap=1: delete all existing sessions
                refreshTokenRepository.deleteAllByUserId(user.getId());
            } else {
                // User cap=5: evict the oldest session
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
