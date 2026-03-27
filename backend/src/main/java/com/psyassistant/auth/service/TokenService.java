package com.psyassistant.auth.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
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
 * <p>Token TTLs differ by role:
 * <ul>
 *   <li>ADMIN: access=15 min, refresh=24 h</li>
 *   <li>USER: access=60 min, refresh=15 d</li>
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
     * @param user the authenticated user
     * @return signed JWT string
     */
    public String buildAccessToken(final User user) {
        Instant now = Instant.now();
        Duration ttl = accessTtlFor(user.getRole());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(ROLES_CLAIM, List.of("ROLE_" + user.getRole().name()))
                .build();

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
     * @param role user role
     * @return refresh token duration
     */
    public Duration refreshTtlFor(final UserRole role) {
        return role == UserRole.ADMIN ? adminRefreshTtl : userRefreshTtl;
    }

    private Duration accessTtlFor(final UserRole role) {
        return role == UserRole.ADMIN ? adminAccessTtl : userAccessTtl;
    }
}
