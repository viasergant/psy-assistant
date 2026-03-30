package com.psyassistant.users;

import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.dto.CreateUserRequest;
import com.psyassistant.users.dto.PatchUserRequest;
import com.psyassistant.users.dto.UserCreationResponseDto;
import com.psyassistant.users.dto.UserPageResponse;
import com.psyassistant.users.dto.UserSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for admin-driven management of internal user accounts.
 *
 * <p>Handles creation, role/status changes, password reset token issuance, and
 * paginated listing.  All state-changing operations produce audit log entries.
 *
 * <p>Raw password reset tokens are never stored — only the SHA-256 hex digest.
 */
@Service
public class UserManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementService.class);

    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String EVENT_USER_CREATED = "USER_CREATED";
    private static final String EVENT_USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    private static final String EVENT_USER_DEACTIVATED = "USER_DEACTIVATED";
    private static final String EVENT_USER_REACTIVATED = "USER_REACTIVATED";
    private static final String EVENT_USER_PASSWORD_RESET_INITIATED = "USER_PASSWORD_RESET_INITIATED";

    /** Number of cryptographically random bytes in a raw reset token. */
    private static final int TOKEN_BYTES = 32;

    /** Password reset token validity window in hours. */
    private static final long RESET_TOKEN_TTL_HOURS = 24L;

    private static final String SHA_256 = "SHA-256";

    /** Placeholder hash used when creating users; forces a password reset before first login. */
    private static final String UNUSABLE_PASSWORD = "UNUSABLE";

    /** Characters allowed in auto-generated temporary passwords. */
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%&*";

    /** Length of auto-generated temporary passwords. */
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs the service.
     *
     * @param userRepository       user data access
     * @param resetTokenRepository password reset token data access
     * @param auditLogService      audit recorder
     * @param passwordEncoder      BCrypt encoder (used to generate an unusable initial hash)
     */
    public UserManagementService(
            final UserRepository userRepository,
            final PasswordResetTokenRepository resetTokenRepository,
            final AuditLogService auditLogService,
            final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns a paginated, optionally filtered list of users.
     *
     * @param role     optional role filter
     * @param active   optional active-status filter
     * @param pageable pagination and sort parameters
     * @return page response with summary DTOs and pagination metadata
     */
    @Transactional(readOnly = true)
    public UserPageResponse listUsers(
            final UserRole role, final Boolean active, final Pageable pageable) {
        UserRole normalizedRole = role == null ? null : role.canonical();
        Page<User> page = userRepository.findAll(
                UserSpecification.withFilters(normalizedRole, active), pageable);
        List<UserSummaryDto> content = page.getContent().stream()
                .map(UserSummaryDto::from)
                .toList();
        return new UserPageResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    /**
     * Creates a new user account with an unusable password and immediately issues a
     * 24-hour password reset token (the raw token is returned for inclusion in the
     * setup email sent by the caller).
     *
     * @param request  validated creation request
     * @param actorId  UUID of the admin performing the action (for audit)
     * @return the created user summary DTO
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public UserSummaryDto createUser(final CreateUserRequest request, final UUID actorId) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        UserRole normalizedRole = request.role().canonical();

        // Store a BCrypt hash of a random string — the user cannot log in until
        // they complete the password reset flow.
        String unusableHash = passwordEncoder.encode(UUID.randomUUID().toString() + UNUSABLE_PASSWORD);
        User user = new User(request.email(), unusableHash, request.fullName(),
                normalizedRole, true);
        userRepository.save(user);

        // Issue a 24h password reset token so the new user can set their password
        String rawToken = generateRawToken();
        storeResetToken(user, rawToken);

        auditLogService.record(new AuditLog.Builder(EVENT_USER_CREATED)
                .userId(actorId)
                .emailAttempted(request.email())
                .outcome(OUTCOME_SUCCESS)
                .detail("targetUserId=" + user.getId() + " requestedRole=" + request.role()
                        + " persistedRole=" + normalizedRole)
                .build());

        LOG.info("event=USER_CREATED actorId={} targetUserId={} requestedRole={} persistedRole={}",
                actorId, user.getId(), request.role(), normalizedRole);

        return UserSummaryDto.from(user);
    }

    /**
     * Applies a partial update to a user account (role, full name, or active status).
     * Only non-null fields in the request are applied.
     *
     * <p>Self-deactivation guard: if the request sets {@code active=false} and the
     * target user is the current principal, a {@link SelfDeactivationException} is thrown.
     *
     * @param userId   target user's UUID
     * @param request  fields to update (all optional)
     * @param actorId  UUID of the admin performing the action (for audit)
     * @return the updated user summary DTO
     * @throws EntityNotFoundException  if no user exists with the given ID
     * @throws SelfDeactivationException if the admin tries to deactivate their own account
     */
    @Transactional
    public UserSummaryDto updateUser(
            final UUID userId, final PatchUserRequest request, final UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }

        if (request.role() != null && !request.role().canonical().equals(user.getRole())) {
            UserRole oldRole = user.getRole();
            UserRole normalizedRole = request.role().canonical();
            user.setRole(normalizedRole);
            auditLogService.record(new AuditLog.Builder(EVENT_USER_ROLE_CHANGED)
                    .userId(actorId)
                    .outcome(OUTCOME_SUCCESS)
                    .detail("targetUserId=" + userId + " oldRole=" + oldRole
                            + " requestedRole=" + request.role()
                            + " newRole=" + normalizedRole)
                    .build());
            LOG.info("event=USER_ROLE_CHANGED actorId={} targetUserId={} {} -> {} (requested={})",
                    actorId, userId, oldRole, normalizedRole, request.role());
        }

        if (request.active() != null && request.active() != user.isActive()) {
            if (Boolean.FALSE.equals(request.active())) {
                // Self-deactivation guard
                if (userId.equals(actorId)) {
                    throw new SelfDeactivationException();
                }
                user.setActive(false);
                auditLogService.record(new AuditLog.Builder(EVENT_USER_DEACTIVATED)
                        .userId(actorId)
                        .outcome(OUTCOME_SUCCESS)
                        .detail("targetUserId=" + userId)
                        .build());
                LOG.info("event=USER_DEACTIVATED actorId={} targetUserId={}", actorId, userId);
            } else {
                user.setActive(true);
                auditLogService.record(new AuditLog.Builder(EVENT_USER_REACTIVATED)
                        .userId(actorId)
                        .outcome(OUTCOME_SUCCESS)
                        .detail("targetUserId=" + userId)
                        .build());
                LOG.info("event=USER_REACTIVATED actorId={} targetUserId={}", actorId, userId);
            }
        }

        userRepository.save(user);
        return UserSummaryDto.from(user);
    }

    /**
     * Generates and stores a 24-hour password reset token for the target user.
     *
     * <p>Any previously issued (but unused) tokens for the user are invalidated.
     * The raw token value is returned so the caller can include it in the reset email;
     * it is not stored anywhere in the system after this method returns.
     *
     * @param userId  target user's UUID
     * @param actorId UUID of the admin initiating the reset (for audit)
     * @return raw reset token (hex string) to embed in the emailed link
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    @Transactional
    public String initiatePasswordReset(final UUID userId, final UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Purge old tokens before issuing a new one
        resetTokenRepository.deleteAllByUserId(userId);

        String rawToken = generateRawToken();
        storeResetToken(user, rawToken);

        auditLogService.record(new AuditLog.Builder(EVENT_USER_PASSWORD_RESET_INITIATED)
                .userId(actorId)
                .outcome(OUTCOME_SUCCESS)
                .detail("targetUserId=" + userId)
                .build());

        LOG.info("event=USER_PASSWORD_RESET_INITIATED actorId={} targetUserId={}",
                actorId, userId);

        return rawToken;
    }

    /**
     * Creates a new user account with an auto-generated temporary password.
     *
     * <p>The plain-text password is returned in the response and must be securely
     * shared with the new user by the admin. The user is flagged with
     * {@code mustChangePassword=true} and will be required to set a permanent
     * password on first login.
     *
     * <p>This method is intended for therapist onboarding workflows where admins
     * create accounts and manually share credentials.
     *
     * @param request  validated creation request
     * @param actorId  UUID of the admin performing the action (for audit)
     * @return creation response including the temporary password
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public UserCreationResponseDto createUserWithTemporaryPassword(
            final CreateUserRequest request, final UUID actorId) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        UserRole normalizedRole = request.role().canonical();

        // Generate secure temporary password
        String temporaryPassword = generateTemporaryPassword();
        String passwordHash = passwordEncoder.encode(temporaryPassword);

        User user = new User(request.email(), passwordHash, request.fullName(),
                normalizedRole, true);
        user.setMustChangePassword(true);
        userRepository.save(user);

        auditLogService.record(new AuditLog.Builder(EVENT_USER_CREATED)
                .userId(actorId)
                .emailAttempted(request.email())
                .outcome(OUTCOME_SUCCESS)
                .detail("targetUserId=" + user.getId() + " requestedRole=" + request.role()
                        + " persistedRole=" + normalizedRole + " temporaryPassword=true")
                .build());

        LOG.info("event=USER_CREATED actorId={} targetUserId={} requestedRole={} "
                + "persistedRole={} temporaryPassword=true",
                actorId, user.getId(), request.role(), normalizedRole);

        return UserCreationResponseDto.from(user, temporaryPassword);
    }

    // ---- private helpers -----------------------------------------------

    /**
     * Generates a cryptographically secure random password (12 characters).
     *
     * <p>Contains mixed case letters, numbers, and special characters to meet
     * typical password complexity requirements. Excludes visually ambiguous
     * characters (0, O, 1, l, I).
     *
     * @return random password string
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = random.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }


    // ---- private helpers -----------------------------------------------

    /**
     * Generates a cryptographically random hex token (32 bytes = 64 hex chars).
     *
     * @return raw hex token
     */
    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Computes SHA-256 of the raw token and stores the record in the DB.
     *
     * @param user     owning user
     * @param rawToken plain-text token (never stored)
     */
    private void storeResetToken(final User user, final String rawToken) {
        String hash = sha256Hex(rawToken);
        Instant expiresAt = Instant.now().plus(RESET_TOKEN_TTL_HOURS, ChronoUnit.HOURS);
        resetTokenRepository.save(new PasswordResetToken(user, hash, expiresAt));
    }

    /**
     * Returns the SHA-256 hex digest of the given string.
     *
     * @param value input string
     * @return hex-encoded SHA-256 hash
     */
    private String sha256Hex(final String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    /**
     * Returns the UUID of the currently authenticated principal, or null if unavailable.
     *
     * @return principal UUID
     */
    public static UUID currentPrincipalId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
