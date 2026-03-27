package com.psyassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.auth.domain.RefreshToken;
import com.psyassistant.auth.domain.RefreshTokenRepository;
import com.psyassistant.auth.dto.LoginRequest;
import com.psyassistant.auth.service.AuthException;
import com.psyassistant.auth.service.AuthResult;
import com.psyassistant.auth.service.AuthService;
import com.psyassistant.auth.service.TokenService;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>Covers: valid login, wrong password, disabled account, session cap for ADMIN and USER.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    private AuthService authService;

    private User activeUser;
    private User adminUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, tokenService,
                passwordEncoder, auditLogService);
        ReflectionTestUtils.setField(authService, "maxAdminSessions", 1);
        ReflectionTestUtils.setField(authService, "maxUserSessions", 5);

        activeUser = makeUser("user@example.com", UserRole.USER, true);
        adminUser = makeUser("admin@example.com", UserRole.ADMIN, true);
        disabledUser = makeUser("disabled@example.com", UserRole.USER, false);
    }

    @Test
    void validCredentialsReturnTokens() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(refreshTokenRepository.countActiveSessions(any())).thenReturn(0L);
        when(tokenService.generateRawRefreshToken()).thenReturn(UUID.randomUUID().toString());
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(tokenService.buildAccessToken(any())).thenReturn("jwt-token");
        when(tokenService.accessTokenExpiresAt(any())).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenService.refreshTtlFor(UserRole.USER)).thenReturn(Duration.ofDays(15));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.authenticate(
                new LoginRequest("user@example.com", "pass"), "127.0.0.1");

        assertThat(result.loginResponse().accessToken()).isEqualTo("jwt-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(result.loginResponse().tokenType()).isEqualTo("Bearer");
    }

    @Test
    void wrongPasswordThrowsInvalidCredentials() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                authService.authenticate(
                        new LoginRequest("user@example.com", "wrong"), "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getCode())
                .isEqualTo(AuthException.ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void unknownEmailThrowsInvalidCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                authService.authenticate(
                        new LoginRequest("ghost@example.com", "pass"), "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getCode())
                .isEqualTo(AuthException.ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void disabledAccountThrowsAccountDisabled() {
        when(userRepository.findByEmail("disabled@example.com"))
                .thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() ->
                authService.authenticate(
                        new LoginRequest("disabled@example.com", "pass"), "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getCode())
                .isEqualTo(AuthException.ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void adminAtCapDeletesAllSessions() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(refreshTokenRepository.countActiveSessions(any())).thenReturn(1L);
        when(tokenService.generateRawRefreshToken()).thenReturn(UUID.randomUUID().toString());
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(tokenService.buildAccessToken(any())).thenReturn("jwt-token");
        when(tokenService.accessTokenExpiresAt(any())).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenService.refreshTtlFor(UserRole.ADMIN)).thenReturn(Duration.ofHours(24));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.authenticate(
                new LoginRequest("admin@example.com", "pass"), "127.0.0.1");

        verify(refreshTokenRepository).deleteAllByUserId(adminUser.getId());
    }

    @Test
    void userUnderCapDoesNotEvict() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(refreshTokenRepository.countActiveSessions(any())).thenReturn(3L);
        when(tokenService.generateRawRefreshToken()).thenReturn(UUID.randomUUID().toString());
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(tokenService.buildAccessToken(any())).thenReturn("jwt-token");
        when(tokenService.accessTokenExpiresAt(any())).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenService.refreshTtlFor(UserRole.USER)).thenReturn(Duration.ofDays(15));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.authenticate(
                new LoginRequest("user@example.com", "pass"), "127.0.0.1");

        verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void userAtCapEvictsOldestSession() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(refreshTokenRepository.countActiveSessions(any())).thenReturn(5L);
        when(tokenService.generateRawRefreshToken()).thenReturn(UUID.randomUUID().toString());
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(tokenService.buildAccessToken(any())).thenReturn("jwt-token");
        when(tokenService.accessTokenExpiresAt(any())).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenService.refreshTtlFor(UserRole.USER)).thenReturn(Duration.ofDays(15));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken oldest = mock(RefreshToken.class);
        when(refreshTokenRepository.findOldestActiveByUserId(any()))
                .thenReturn(List.of(oldest));

        authService.authenticate(
                new LoginRequest("user@example.com", "pass"), "127.0.0.1");

        verify(refreshTokenRepository).delete(oldest);
    }

    @Test
    void validRefreshTokenRotatesAndReturnsNewTokens() {
        RefreshToken stored = mock(RefreshToken.class);
        when(stored.getUser()).thenReturn(activeUser);
        when(refreshTokenRepository.findActiveByHash(anyString()))
                .thenReturn(Optional.of(stored));
        when(tokenService.generateRawRefreshToken()).thenReturn(UUID.randomUUID().toString());
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(tokenService.buildAccessToken(any())).thenReturn("jwt-token");
        when(tokenService.accessTokenExpiresAt(any())).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenService.refreshTtlFor(UserRole.USER)).thenReturn(Duration.ofDays(15));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = authService.refresh("raw-token", "127.0.0.1");

        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
        assertThat(result.loginResponse().accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void invalidRefreshTokenThrowsTokenExpired() {
        when(refreshTokenRepository.findActiveByHash(anyString()))
                .thenReturn(Optional.empty());
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));

        assertThatThrownBy(() -> authService.refresh("bad-token", "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getCode())
                .isEqualTo(AuthException.ErrorCode.TOKEN_EXPIRED);
    }

    /**
     * Bug fix AC: a deactivated user with a valid refresh token must be rejected
     * at the refresh endpoint — the old token must be deleted and TOKEN_EXPIRED thrown.
     */
    @Test
    void refreshWithDeactivatedUserRejectsAndDeletesToken() {
        RefreshToken stored = mock(RefreshToken.class);
        when(stored.getUser()).thenReturn(disabledUser);
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(refreshTokenRepository.findActiveByHash(anyString()))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("raw-token", "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getCode())
                .isEqualTo(AuthException.ErrorCode.TOKEN_EXPIRED);

        // The stale token must be invalidated to prevent replay attacks
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void logoutWithValidTokenDeletesSession() {
        RefreshToken stored = mock(RefreshToken.class);
        when(stored.getUser()).thenReturn(activeUser);
        when(tokenService.hashRefreshToken(anyString())).thenReturn("deadbeef".repeat(8));
        when(refreshTokenRepository.findActiveByHash(anyString()))
                .thenReturn(Optional.of(stored));

        authService.logout("raw-token", "127.0.0.1");

        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void logoutWithNullTokenDoesNothing() {
        authService.logout(null, "127.0.0.1");
        verify(refreshTokenRepository, never()).findActiveByHash(any());
    }

    // ---- helpers -------------------------------------------------------

    private User makeUser(final String email, final UserRole role, final boolean active) {
        User user = new User(email, "hashed-pw", role, active);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
