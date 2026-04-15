package com.psyassistant.auth.service;

import com.psyassistant.auth.domain.RefreshTokenRepository;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityProperties;
import com.psyassistant.notifications.EmailMessage;
import com.psyassistant.notifications.EmailNotificationPort;
import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.users.PasswordResetToken;
import com.psyassistant.users.PasswordResetTokenRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the self-service password reset flow.
 *
 * <p>Token entropy: 32 random bytes (256 bits) encoded as a 64-char hex string.
 * Only the SHA-256 digest is persisted — the raw token travels only inside the email link.
 *
 * <p>Anti-enumeration: {@link #requestReset} always returns void; email is sent only
 * if the address belongs to a registered user.
 */
@Service
public class PasswordResetService {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);

    private static final String SHA_256 = "SHA-256";
    private static final int TOKEN_BYTES = 32;
    private static final String EVENT_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    private static final String EVENT_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";
    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_FAILURE = "FAILURE";

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordComplexityValidator complexityValidator;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailNotificationPort emailNotificationPort;
    private final SecurityProperties securityProperties;

    /**
     * Constructs the service with all required collaborators.
     *
     * @param userRepository        user data access
     * @param resetTokenRepository  password reset token data access
     * @param refreshTokenRepository refresh token data access (for session invalidation)
     * @param complexityValidator   password policy validator
     * @param passwordEncoder       BCrypt encoder
     * @param auditLogService       audit log recorder
     * @param emailNotificationPort outbound email port
     * @param securityProperties    security policy configuration
     */
    public PasswordResetService(
            final UserRepository userRepository,
            final PasswordResetTokenRepository resetTokenRepository,
            final RefreshTokenRepository refreshTokenRepository,
            final PasswordComplexityValidator complexityValidator,
            final PasswordEncoder passwordEncoder,
            final AuditLogService auditLogService,
            final EmailNotificationPort emailNotificationPort,
            final SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.complexityValidator = complexityValidator;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.emailNotificationPort = emailNotificationPort;
        this.securityProperties = securityProperties;
    }

    /**
     * Requests a password reset for the given email address.
     *
     * <p>Always returns without error regardless of whether the email is registered
     * (anti-enumeration). If the user is found, old unused tokens are purged, a new
     * token is created and stored (hashed), and a reset email is queued.
     *
     * @param email address to send the reset link to
     */
    @Transactional
    public void requestReset(final String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            resetTokenRepository.deleteAllByUserId(user.getId());

            String rawToken = generateRawToken();
            String tokenHash = sha256Hex(rawToken);
            long ttlHours = securityProperties.passwordReset().tokenTtlHours();
            Instant expiresAt = Instant.now().plus(Duration.ofHours(ttlHours));
            resetTokenRepository.save(new PasswordResetToken(user, tokenHash, expiresAt));

            String resetLink = frontendBaseUrl + "/auth/reset-password?token=" + rawToken;
            try {
                emailNotificationPort.queue(new EmailMessage(
                        email,
                        NotificationEventType.PASSWORD_RESET,
                        "email.subject.password.reset",
                        "Click the link below to reset your password (valid for " + ttlHours
                                + " hours):\n\n" + resetLink));
            } catch (Exception ex) {
                LOG.warn("event=PASSWORD_RESET_EMAIL_FAILED userId={} reason={}",
                        user.getId(), ex.getMessage());
            }

            auditLogService.record(new AuditLog.Builder(EVENT_RESET_REQUESTED)
                    .userId(user.getId())
                    .outcome(OUTCOME_SUCCESS)
                    .build());
            LOG.info("event=PASSWORD_RESET_REQUESTED userId={}", user.getId());
        });
    }

    /**
     * Confirms a password reset: validates the token, enforces password complexity,
     * updates the password, marks the token as used, and invalidates all active sessions.
     *
     * @param rawToken    the raw token from the email link
     * @param newPassword the new password chosen by the user
     * @throws AuthException          if the token is invalid or expired
     * @throws PasswordPolicyException if the new password violates complexity rules
     */
    @Transactional
    public void confirmReset(final String rawToken, final String newPassword) {
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken token = resetTokenRepository.findByTokenHash(tokenHash)
                .orElse(null);

        if (token == null || token.isUsed()) {
            throw new AuthException(AuthException.ErrorCode.TOKEN_INVALID,
                    "Reset token is invalid or has already been used");
        }

        if (!token.isValid()) {
            throw new AuthException(AuthException.ErrorCode.TOKEN_EXPIRED,
                    "Reset token has expired. Please request a new password reset link.");
        }

        List<String> violations = complexityValidator.validate(newPassword);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(violations);
        }

        User user = token.getUser();
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed();

        // Invalidate all active sessions so the user must log in with the new password
        refreshTokenRepository.deleteAllByUserId(user.getId());

        auditLogService.record(new AuditLog.Builder(EVENT_RESET_COMPLETED)
                .userId(user.getId())
                .outcome(OUTCOME_SUCCESS)
                .build());
        LOG.info("event=PASSWORD_RESET_COMPLETED userId={}", user.getId());
    }

    // ---- helpers -------------------------------------------------------

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(final String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
