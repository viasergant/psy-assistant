package com.psyassistant.common.mode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.auth.token.InvalidTokenException;
import com.psyassistant.auth.token.OneTimeTokenService;
import com.psyassistant.auth.token.TokenPurpose;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.captcha.CaptchaVerifier;
import com.psyassistant.notifications.EmailMessage;
import com.psyassistant.notifications.EmailNotificationPort;
import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class RegistrationServiceTest {

    private static final String EMAIL = "new@example.com";
    private static final String IP = "203.0.113.7";

    private OrganizationRepository orgRepo;
    private UserRepository userRepo;
    private OneTimeTokenService tokens;
    private EmailNotificationPort email;
    private CaptchaVerifier captcha;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        orgRepo = mock(OrganizationRepository.class);
        userRepo = mock(UserRepository.class);
        tokens = mock(OneTimeTokenService.class);
        email = mock(EmailNotificationPort.class);
        captcha = mock(CaptchaVerifier.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hash");
        when(captcha.verify(any(), any())).thenReturn(true);
        when(orgRepo.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokens.issue(any(), eq(TokenPurpose.EMAIL_CONFIRMATION), any(Duration.class)))
                .thenReturn("rawtoken");
        service = new RegistrationService(orgRepo, userRepo, encoder, mock(AuditLogService.class),
                tokens, email, captcha, new ResendRateLimiter());
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "https://app.test");
    }

    private RegistrationRequest request() {
        return new RegistrationRequest("My Practice", EMAIL, "Passw0rd!", "captcha");
    }

    @Test
    void registerNewEmailCreatesInactiveUnverifiedUserAndSendsLink() {
        service.register(request(), IP);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(user.capture());
        assertThat(user.getValue().isActive()).isFalse();
        assertThat(user.getValue().isEmailVerified()).isFalse();
        ArgumentCaptor<EmailMessage> msg = ArgumentCaptor.forClass(EmailMessage.class);
        verify(email).queue(msg.capture());
        assertThat(msg.getValue().eventType()).isEqualTo(NotificationEventType.EMAIL_CONFIRMATION);
        assertThat(msg.getValue().recipientAddress()).isEqualTo(EMAIL);
        assertThat(msg.getValue().renderedBody())
                .contains("https://app.test/auth/confirm-email?token=rawtoken");
    }

    @Test
    void registerExistingEmailCreatesNothingAndNotifiesOwner() {
        when(userRepo.existsByEmail(EMAIL)).thenReturn(true);

        service.register(request(), IP);

        verify(orgRepo, never()).save(any());
        verify(userRepo, never()).save(any());
        ArgumentCaptor<EmailMessage> msg = ArgumentCaptor.forClass(EmailMessage.class);
        verify(email).queue(msg.capture());
        assertThat(msg.getValue().eventType())
                .isEqualTo(NotificationEventType.REGISTRATION_ATTEMPT_EXISTING);
    }

    @Test
    void registerCaptchaFailsIsRejectedBeforeAnyWork() {
        when(captcha.verify(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.register(request(), IP))
                .isInstanceOf(CaptchaFailedException.class);
        verify(userRepo, never()).existsByEmail(anyString());
    }

    @Test
    void registerEmailQueueFailureDoesNotBreakResponse() {
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp")).when(email).queue(any());

        service.register(request(), IP);

        verify(userRepo).save(any(User.class));
    }

    @Test
    void confirmValidTokenActivatesUser() {
        User user = unverifiedUser();
        UUID id = UUID.randomUUID();
        when(tokens.consume("raw", TokenPurpose.EMAIL_CONFIRMATION)).thenReturn(id);
        when(userRepo.findById(id)).thenReturn(Optional.of(user));

        service.confirm("raw");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void confirmInvalidTokenPropagates() {
        when(tokens.consume(any(), any())).thenThrow(
                new InvalidTokenException(InvalidTokenException.Reason.INVALID));

        assertThatThrownBy(() -> service.confirm("bad")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resendUnverifiedUserSendsNewLink() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));

        service.resendConfirmation(new ResendConfirmationRequest(EMAIL, "c"), IP);

        verify(email).queue(any(EmailMessage.class));
    }

    @Test
    void resendVerifiedOrUnknownUserIsSilent() {
        User verified = unverifiedUser();
        verified.setEmailVerified(true);
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(verified));
        service.resendConfirmation(new ResendConfirmationRequest(EMAIL, "c"), IP);

        when(userRepo.findByEmail("who@example.com")).thenReturn(Optional.empty());
        service.resendConfirmation(new ResendConfirmationRequest("who@example.com", "c"), IP);

        verify(email, never()).queue(any());
    }

    @Test
    void resendOverLimitForSameEmailIsRejected() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
        ResendConfirmationRequest req = new ResendConfirmationRequest(EMAIL, "c");

        for (int i = 0; i < ResendRateLimiter.MAX_PER_WINDOW; i++) {
            service.resendConfirmation(req, "10.0.0." + i);
        }

        assertThatThrownBy(() -> service.resendConfirmation(req, "10.0.1.1"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    private User unverifiedUser() {
        User user = new User(EMAIL, "hash", Set.of(UserRole.THERAPIST), false);
        user.setEmailVerified(false);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
