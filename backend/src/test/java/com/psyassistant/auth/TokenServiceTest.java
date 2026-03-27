package com.psyassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.psyassistant.auth.service.TokenService;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link TokenService}.
 *
 * <p>Verifies JWT claim correctness, HS256 signature, TTL-by-role, and
 * refresh token hashing behaviour.
 */
@SpringBootTest
@ActiveProfiles("test")
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("admin@example.com", "hash", UserRole.ADMIN, true);
        setUserId(adminUser, UUID.randomUUID());

        regularUser = new User("user@example.com", "hash", UserRole.USER, true);
        setUserId(regularUser, UUID.randomUUID());
    }

    @Test
    void buildAccessTokenForAdminContainsCorrectClaims() {
        String token = tokenService.buildAccessToken(adminUser);

        var jwt = jwtDecoder.decode(token);

        assertThat(jwt.getClaimAsString("iss")).isEqualTo("psy-assistant");
        assertThat(jwt.getSubject()).isEqualTo(adminUser.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void adminTokenExpiresIn15Minutes() {
        Instant before = Instant.now();
        String token = tokenService.buildAccessToken(adminUser);
        var jwt = jwtDecoder.decode(token);

        Duration diff = Duration.between(before, jwt.getExpiresAt());
        assertThat(diff).isBetween(Duration.ofMinutes(14), Duration.ofMinutes(16));
    }

    @Test
    void userTokenExpiresIn60Minutes() {
        Instant before = Instant.now();
        String token = tokenService.buildAccessToken(regularUser);
        var jwt = jwtDecoder.decode(token);

        Duration diff = Duration.between(before, jwt.getExpiresAt());
        assertThat(diff).isBetween(Duration.ofMinutes(59), Duration.ofMinutes(61));
    }

    @Test
    void adminRefreshTtlIs24Hours() {
        Duration ttl = tokenService.refreshTtlFor(UserRole.ADMIN);
        assertThat(ttl).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void userRefreshTtlIs15Days() {
        Duration ttl = tokenService.refreshTtlFor(UserRole.USER);
        assertThat(ttl).isEqualTo(Duration.ofDays(15));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        String hash1 = tokenService.hashRefreshToken("token-one");
        String hash2 = tokenService.hashRefreshToken("token-two");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void sameInputProducesSameHash() {
        String raw = UUID.randomUUID().toString();
        assertThat(tokenService.hashRefreshToken(raw))
                .isEqualTo(tokenService.hashRefreshToken(raw));
    }

    @Test
    void hashOutputIs64HexChars() {
        String hash = tokenService.hashRefreshToken("any-token");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void rawRefreshTokenIsUuidFormat() {
        String raw = tokenService.generateRawRefreshToken();
        // Should not throw
        UUID.fromString(raw);
    }

    @Test
    void shortSecretThrowsIllegalState() {
        TokenService svc = createServiceWithSecret("short");
        assertThatThrownBy(svc::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    // ---- helpers -------------------------------------------------------

    private void setUserId(final User user, final UUID id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TokenService createServiceWithSecret(final String secret) {
        TokenService svc = new TokenService();
        try {
            setField(svc, "jwtSecret", secret);
            setField(svc, "issuer", "psy-assistant");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return svc;
    }

    private void setField(final Object target, final String name, final Object value)
            throws Exception {
        java.lang.reflect.Field field = TokenService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
