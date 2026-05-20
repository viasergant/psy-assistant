package com.psyassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.psyassistant.auth.service.TokenService;
import com.psyassistant.common.security.Permission;
import com.psyassistant.common.security.RolePermissions;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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

    private User sysAdminUser;
    private User therapistUser;

    @BeforeEach
    void setUp() {
        sysAdminUser = new User("admin@example.com", "hash", Set.of(UserRole.SYSTEM_ADMINISTRATOR), true);
        setUserId(sysAdminUser, UUID.randomUUID());

        therapistUser = new User("user@example.com", "hash", Set.of(UserRole.THERAPIST), true);
        setUserId(therapistUser, UUID.randomUUID());
    }

    @Test
    void buildAccessTokenForSystemAdminContainsRoleAuthority() {
        String token = tokenService.buildAccessToken(sysAdminUser);

        var jwt = jwtDecoder.decode(token);

        assertThat(jwt.getClaimAsString("iss")).isEqualTo("psy-assistant");
        assertThat(jwt.getSubject()).isEqualTo(sysAdminUser.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).contains("ROLE_SYSTEM_ADMINISTRATOR");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void buildAccessTokenForSystemAdminContainsPermissions() {
        String token = tokenService.buildAccessToken(sysAdminUser);
        var jwt = jwtDecoder.decode(token);

        List<String> roles = jwt.getClaimAsStringList("roles");
        // SYSTEM_ADMINISTRATOR should have MANAGE_USERS and VIEW_AUDIT_LOG
        assertThat(roles).contains(
                Permission.MANAGE_USERS.name(),
                Permission.VIEW_AUDIT_LOG.name(),
                Permission.MANAGE_SYSTEM_CONFIG.name()
        );
    }

    @Test
    void buildAccessTokenForTherapistContainsCorrectPermissions() {
        String token = tokenService.buildAccessToken(therapistUser);
        var jwt = jwtDecoder.decode(token);

        List<String> roles = jwt.getClaimAsStringList("roles");
        assertThat(roles).contains("ROLE_THERAPIST");
        // THERAPIST permissions
        assertThat(roles).contains(
                Permission.READ_OWN_SESSIONS.name(),
                Permission.WRITE_SESSION_NOTE.name(),
                Permission.READ_OWN_SESSION_NOTES.name(),
                Permission.READ_CARE_PLANS.name()
        );
        // THERAPIST must NOT have admin permissions
        assertThat(roles).doesNotContain(
                Permission.MANAGE_USERS.name(),
                Permission.VIEW_AUDIT_LOG.name(),
                Permission.READ_ALL_SESSIONS.name()
        );
    }

    @Test
    void rolesClaimContainsExactlyRoleAndPermissionsForRole() {
        String token = tokenService.buildAccessToken(therapistUser);
        var jwt = jwtDecoder.decode(token);

        List<String> roles = jwt.getClaimAsStringList("roles");
        // Should contain ROLE_THERAPIST + all THERAPIST permissions
        int expectedSize = 1 + RolePermissions.permissionsFor(UserRole.THERAPIST).size();
        assertThat(roles).hasSize(expectedSize);
    }

    @Test
    void systemAdminTokenExpiresIn15Minutes() {
        Instant before = Instant.now();
        String token = tokenService.buildAccessToken(sysAdminUser);
        var jwt = jwtDecoder.decode(token);

        Duration diff = Duration.between(before, jwt.getExpiresAt());
        assertThat(diff).isBetween(Duration.ofMinutes(14), Duration.ofMinutes(16));
    }

    @Test
    void therapistTokenExpiresIn60Minutes() {
        Instant before = Instant.now();
        String token = tokenService.buildAccessToken(therapistUser);
        var jwt = jwtDecoder.decode(token);

        Duration diff = Duration.between(before, jwt.getExpiresAt());
        assertThat(diff).isBetween(Duration.ofMinutes(59), Duration.ofMinutes(61));
    }

    @Test
    void systemAdminRefreshTtlIs24Hours() {
        Duration ttl = tokenService.refreshTtlFor(Set.of(UserRole.SYSTEM_ADMINISTRATOR));
        assertThat(ttl).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void therapistRefreshTtlIs15Days() {
        Duration ttl = tokenService.refreshTtlFor(Set.of(UserRole.THERAPIST));
        assertThat(ttl).isEqualTo(Duration.ofDays(15));
    }

    @Test
    void receptionAdminStaffRefreshTtlIs15Days() {
        Duration ttl = tokenService.refreshTtlFor(Set.of(UserRole.RECEPTION_ADMIN_STAFF));
        assertThat(ttl).isEqualTo(Duration.ofDays(15));
    }

    @Test
    void financeRefreshTtlIs15Days() {
        Duration ttl = tokenService.refreshTtlFor(Set.of(UserRole.FINANCE));
        assertThat(ttl).isEqualTo(Duration.ofDays(15));
    }

    @Test
    void supervisorRefreshTtlIs15Days() {
        Duration ttl = tokenService.refreshTtlFor(Set.of(UserRole.SUPERVISOR));
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

    @Test
    void buildAccessTokenForUserWithTwoRolesContainsBothRoleClaimsAndUnionPermissions() {
        // Arrange: user with THERAPIST + SUPERVISOR
        User multiRoleUser = new User("multi@example.com", "hash",
                Set.of(UserRole.THERAPIST, UserRole.SUPERVISOR), true);
        setUserId(multiRoleUser, UUID.randomUUID());

        // Act
        String token = tokenService.buildAccessToken(multiRoleUser);
        var jwt = jwtDecoder.decode(token);
        List<String> claims = jwt.getClaimAsStringList("roles");

        // Assert: both ROLE_ entries present
        assertThat(claims).contains("ROLE_THERAPIST", "ROLE_SUPERVISOR");

        // Assert: union of permissions from both roles is present
        Set<Permission> therapistPerms = RolePermissions.permissionsFor(UserRole.THERAPIST);
        Set<Permission> supervisorPerms = RolePermissions.permissionsFor(UserRole.SUPERVISOR);
        for (Permission p : therapistPerms) {
            assertThat(claims).contains(p.name());
        }
        for (Permission p : supervisorPerms) {
            assertThat(claims).contains(p.name());
        }

        // Assert: no duplicate entries in the claim
        assertThat(claims).doesNotHaveDuplicates();

        // Assert: size equals 2 role prefixes + union of distinct permissions
        Set<Permission> unionPerms = new java.util.HashSet<>(therapistPerms);
        unionPerms.addAll(supervisorPerms);
        int expectedSize = 2 + unionPerms.size();
        assertThat(claims).hasSize(expectedSize);
    }

    @Test
    void accessTtlUsesAdminTtlWhenOneRoleIsSysAdmin() {
        // Arrange: user with THERAPIST + SYSTEM_ADMINISTRATOR
        Set<UserRole> roles = Set.of(UserRole.THERAPIST, UserRole.SYSTEM_ADMINISTRATOR);

        // Act
        Duration ttl = tokenService.accessTtlFor(roles);

        // Assert: admin TTL (15 min) applies because SYSTEM_ADMINISTRATOR is present
        assertThat(ttl).isEqualTo(Duration.ofMinutes(15));
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
