package com.psyassistant.common.mode;

import com.psyassistant.auth.token.InvalidTokenException;
import com.psyassistant.auth.token.OneTimeTokenService;
import com.psyassistant.auth.token.TokenPurpose;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.captcha.CaptchaVerifier;
import com.psyassistant.notifications.EmailMessage;
import com.psyassistant.notifications.EmailNotificationPort;
import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.organization.OrganizationStatus;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.therapists.service.SingleTherapistProfileProvisioner;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles self-service registration of new solo therapist organizations.
 *
 * <p>Registration creates an {@link Organization} (type SOLO) and an <em>inactive,
 * unverified</em> owner {@link User}, then emails a single-use confirmation link. No
 * tokens are issued until the address is confirmed; confirming activates the account.
 *
 * <p>Anti-enumeration: registering an already-used address looks identical to the caller
 * (same response, same work); the existing owner is told by email instead.
 *
 * <p>Email uniqueness is enforced globally here: the login flow identifies users by email.
 */
@Service
@Profile("saas")
public class RegistrationService {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    private static final Duration CONFIRMATION_TTL = Duration.ofHours(24);
    private static final String CONFIRM_PATH = "/auth/confirm-email?token=";

    private static final Set<UserRole> OWNER_ROLES = EnumSet.of(
            UserRole.RECEPTION_ADMIN_STAFF,
            UserRole.THERAPIST,
            UserRole.SUPERVISOR,
            UserRole.FINANCE,
            UserRole.SYSTEM_ADMINISTRATOR
    );

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final OneTimeTokenService tokenService;
    private final EmailNotificationPort emailNotificationPort;
    private final CaptchaVerifier captchaVerifier;
    private final ResendRateLimiter rateLimiter;
    private final SingleTherapistProfileProvisioner therapistProvisioner;

    /**
     * Constructs the service.
     *
     * @param organizationRepository organization data access
     * @param userRepository         user data access
     * @param passwordEncoder        BCrypt encoder
     * @param auditLogService        audit recorder
     * @param tokenService           one-time token issuer
     * @param emailNotificationPort  outbound email queue
     * @param captchaVerifier        CAPTCHA verifier
     * @param rateLimiter            limiter for emails sent to a single address
     * @param therapistProvisioner   creates the owner's therapist profile
     */
    public RegistrationService(final OrganizationRepository organizationRepository,
                               final UserRepository userRepository,
                               final PasswordEncoder passwordEncoder,
                               final AuditLogService auditLogService,
                               final OneTimeTokenService tokenService,
                               final EmailNotificationPort emailNotificationPort,
                               final CaptchaVerifier captchaVerifier,
                               final ResendRateLimiter rateLimiter,
                               final SingleTherapistProfileProvisioner therapistProvisioner) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.tokenService = tokenService;
        this.emailNotificationPort = emailNotificationPort;
        this.captchaVerifier = captchaVerifier;
        this.rateLimiter = rateLimiter;
        this.therapistProvisioner = therapistProvisioner;
    }

    /**
     * Registers a new solo therapist organization and sends a confirmation email.
     *
     * <p>Returns normally whether or not the email was already registered.
     *
     * @param request  validated registration data
     * @param remoteIp caller IP (for CAPTCHA and rate limiting)
     * @throws CaptchaFailedException  if the CAPTCHA check fails
     * @throws TooManyRequestsException if the caller IP is over the limit
     */
    @Transactional
    public void register(final RegistrationRequest request, final String remoteIp) {
        requireCaptchaAndIpBudget(request.captchaToken(), remoteIp);

        // Always hash, so existing and new addresses cost the same.
        String hash = passwordEncoder.encode(request.password());

        if (userRepository.existsByEmail(request.ownerEmail())) {
            notifyExistingOwner(request.ownerEmail());
            return;
        }

        String slug = generateUniqueSlug(request.orgName());
        Organization org = new Organization(
                request.orgName(), slug, OrganizationType.SOLO, OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(org);

        User user = new User(request.ownerEmail(), hash, OWNER_ROLES, false);
        user.setEmailVerified(false);
        user.setOrgId(savedOrg.getId());
        User savedUser = userRepository.save(user);
        therapistProvisioner.ensureProfile(savedUser);

        sendConfirmationEmail(savedUser);

        LOG.info("event=ORG_REGISTERED orgId={} userId={}", savedOrg.getId(), savedUser.getId());
        audit("ORG_REGISTRATION", savedUser, "orgId=" + savedOrg.getId() + " slug=" + slug);
    }

    /**
     * Confirms the email address and activates the account.
     *
     * @param rawToken token from the confirmation link
     * @throws InvalidTokenException if the token is unknown, used, expired or of another purpose
     */
    @Transactional
    public void confirm(final String rawToken) {
        UUID userId = tokenService.consume(rawToken, TokenPurpose.EMAIL_CONFIRMATION);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException(InvalidTokenException.Reason.INVALID));
        user.setEmailVerified(true);
        user.setActive(true);
        LOG.info("event=EMAIL_CONFIRMED userId={}", user.getId());
        audit("EMAIL_CONFIRMED", user, null);
    }

    /**
     * Re-sends the confirmation email if the address belongs to an unverified account.
     *
     * <p>Silent for unknown or already-verified addresses (anti-enumeration).
     *
     * @param request  validated request
     * @param remoteIp caller IP
     * @throws CaptchaFailedException   if the CAPTCHA check fails
     * @throws TooManyRequestsException if the address or IP is over the limit
     */
    @Transactional
    public void resendConfirmation(final ResendConfirmationRequest request, final String remoteIp) {
        requireCaptchaAndIpBudget(request.captchaToken(), remoteIp);
        if (!rateLimiter.tryAcquire("resend-email:" + sha256Hex(normalize(request.email())))) {
            throw new TooManyRequestsException("Too many confirmation emails requested");
        }
        userRepository.findByEmail(request.email())
                .filter(u -> !u.isEmailVerified())
                .ifPresent(this::sendConfirmationEmail);
    }

    private void requireCaptchaAndIpBudget(final String captchaToken, final String remoteIp) {
        if (!captchaVerifier.verify(captchaToken, remoteIp)) {
            throw new CaptchaFailedException();
        }
        if (remoteIp != null && !rateLimiter.tryAcquire("ip:" + remoteIp)) {
            throw new TooManyRequestsException("Too many requests from this address");
        }
    }

    private void sendConfirmationEmail(final User user) {
        String raw = tokenService.issue(user.getId(), TokenPurpose.EMAIL_CONFIRMATION, CONFIRMATION_TTL);
        String link = frontendBaseUrl + CONFIRM_PATH + raw;
        queueSafely(new EmailMessage(
                user.getEmail(),
                NotificationEventType.EMAIL_CONFIRMATION,
                "email.subject.email.confirmation",
                "Confirm your email address to activate your account (link valid for "
                        + CONFIRMATION_TTL.toHours() + " hours):\n\n" + link
                        + "\n\nIf you did not register, you can ignore this message."));
    }

    private void notifyExistingOwner(final String email) {
        if (!rateLimiter.tryAcquire("notice:" + sha256Hex(normalize(email)))) {
            return;
        }
        queueSafely(new EmailMessage(
                email,
                NotificationEventType.REGISTRATION_ATTEMPT_EXISTING,
                "email.subject.registration.attempt",
                "Someone tried to register a new account with this email address. "
                        + "An account already exists - if it was you, simply log in "
                        + "(or reset your password). Otherwise you can ignore this message."));
    }

    private void queueSafely(final EmailMessage message) {
        try {
            emailNotificationPort.queue(message);
        } catch (Exception ex) {
            // Never reveal delivery problems to the caller; the user can request a resend.
            LOG.warn("event=REGISTRATION_EMAIL_QUEUE_FAILED type={} reason={}",
                    message.eventType(), ex.getClass().getSimpleName());
        }
    }

    private void audit(final String event, final User user, final String detail) {
        try {
            AuditLog.Builder builder = new AuditLog.Builder(event)
                    .userId(user.getId())
                    .outcome("SUCCESS");
            if (detail != null) {
                builder.detail(detail);
            }
            auditLogService.record(builder.build());
        } catch (Exception ex) {
            LOG.warn("Audit log failed: {}", ex.getMessage());
        }
    }

    private static String normalize(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256Hex(final String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String generateUniqueSlug(final String orgName) {
        String base = orgName.toLowerCase(Locale.ROOT)
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
