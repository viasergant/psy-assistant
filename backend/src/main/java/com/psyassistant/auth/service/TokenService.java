package com.psyassistant.auth.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.psyassistant.common.security.Permission;
import com.psyassistant.common.security.RolePermissions;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Builds signed JWT access tokens and manages refresh token raw values.
 *
 * <p>Access tokens are HS256-signed JWTs. Refresh tokens are random UUIDs that
 * are stored only as their SHA-256 hex digests in the database.
 *
 * <p>The {@code roles} claim in the JWT contains both the {@code ROLE_X} value
 * (consumed by Spring Security's {@code hasRole()} checks in the filter chain)
 * and all {@link Permission} names for the role (consumed by
 * {@code hasAuthority()} checks in {@code @PreAuthorize} annotations).
 *
 * <p>Token TTLs by role:
 * <ul>
 *   <li>SYSTEM_ADMINISTRATOR: access=15 min, refresh=24 h</li>
 *   <li>All other roles: access=60 min, refresh=15 d</li>
 * </ul>
 */
@Service
public class TokenService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);

    private static final String SHA_256 = "SHA-256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String ROLES_CLAIM = "roles";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.security.token-ttl.admin.access}")
    private Duration adminAccessTtl;

    @Value("${app.security.token-ttl.admin.refresh}")
    private Duration adminRefreshTtl;

    @Value("${app.security.token-ttl.user.access}")
    private Duration userAccessTtl;

    @Value("${app.security.token-ttl.user.refresh}")
    private Duration userRefreshTtl;

    private JwtEncoder jwtEncoder;

    /**
     * Validates the JWT secret length and initialises the encoder.
     * Called automatically after Spring injects all {@code @Value} fields.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        SecretKey key = new SecretKeySpec(keyBytes, HMAC_SHA256);
        jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        LOG.info("TokenService initialised; issuer={}", issuer);
    }

    /**
     * Builds and signs a JWT access token for the given user.
     *
     * <p>The {@code roles} claim contains:
     * <ol>
     *   <li>The {@code ROLE_X} value for the user's role (for {@code hasRole()} checks)</li>
     *   <li>All {@link Permission} names granted to the role (for {@code hasAuthority()} checks)</li>
     * </ol>
     *
     * @param user the authenticated user
     * @return signed JWT string
     */
    public String buildAccessToken(final User user) {
        return buildAccessToken(user, null);
    }

    /**
     * Builds and signs a JWT access token for the given user with optional therapist profile ID.
     *
     * <p>The {@code roles} claim contains:
     * <ol>
     *   <li>The {@code ROLE_X} value for the user's role (for {@code hasRole()} checks)</li>
     *   <li>All {@link Permission} names granted to the role (for {@code hasAuthority()} checks)</li>
     * </ol>
     *
     * <p>If the therapistProfileId is provided, it is included as a claim for therapist-specific operations.
     *
     * @param user the authenticated user
     * @param therapistProfileId optional therapist profile UUID (for THERAPIST role users)
     * @return signed JWT string
     */
    public String buildAccessToken(final User user, final UUID therapistProfileId) {
        Instant now = Instant.now();
        Duration ttl = accessTtlFor(user.getRole());

        List<String> authorities = buildAuthorities(user.getRole());

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(ROLES_CLAIM, authorities);

        if (therapistProfileId != null) {
            claimsBuilder.claim("therapistProfileId", therapistProfileId.toString());
        }

        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Returns when an access token built now for the given role will expire.
     *
     * @param role user role
     * @return expiry instant
     */
    public Instant accessTokenExpiresAt(final UserRole role) {
        return Instant.now().plus(accessTtlFor(role));
    }

    /**
     * Generates a raw refresh token (UUID string) for use as a cookie value.
     *
     * @return random UUID string
     */
    public String generateRawRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the SHA-256 hex digest of the raw refresh token for DB storage.
     *
     * @param rawToken the plain UUID token string
     * @return hex-encoded SHA-256 digest (64 chars)
     */
    public String hashRefreshToken(final String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    /**
     * Returns the refresh TTL for the given role.
     *
     * <p>SYSTEM_ADMINISTRATOR tokens expire in 24 hours; all other roles in 15 days.
     *
     * @param role user role
     * @return refresh token duration
     */
    public Duration refreshTtlFor(final UserRole role) {
        return role == UserRole.SYSTEM_ADMINISTRATOR || role == UserRole.ADMIN
                ? adminRefreshTtl
                : userRefreshTtl;
    }

    // ---- private helpers -------------------------------------------------

    /**
     * Builds the full authority list for the JWT {@code roles} claim.
     *
     * <p>The list starts with {@code ROLE_X} and is followed by all
     * {@link Permission} names (without any {@code ROLE_} prefix) from
     * {@link RolePermissions}.
     *
     * @param role the user role
     * @return mutable list of authority strings for the JWT claim
     */
    private List<String> buildAuthorities(final UserRole role) {
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + role.name());
        for (Permission permission : RolePermissions.permissionsFor(role)) {
            authorities.add(permission.name());
        }
        return authorities;
    }

    private Duration accessTtlFor(final UserRole role) {
        return role == UserRole.SYSTEM_ADMINISTRATOR || role == UserRole.ADMIN
                ? adminAccessTtl
                : userAccessTtl;
    }
}
