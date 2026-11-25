package com.psyassistant.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and consumes single-use tokens (256-bit random, stored as SHA-256 hash).
 *
 * <p>Generic by design: new {@link TokenPurpose} values can reuse it (e.g. password reset).
 */
@Service
public class OneTimeTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String SHA_256 = "SHA-256";

    private final OneTimeTokenRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /**
     * Production constructor.
     *
     * @param repository token store
     */
    @Autowired
    public OneTimeTokenService(final OneTimeTokenRepository repository) {
        this(repository, Clock.systemUTC());
    }

    OneTimeTokenService(final OneTimeTokenRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Issues a new token, invalidating earlier ones for the same user and purpose.
     *
     * @param userId  token owner
     * @param purpose intended use
     * @param ttl     validity period
     * @return the raw token; it is never stored and must be delivered to the user only
     */
    @Transactional
    public String issue(final UUID userId, final TokenPurpose purpose, final Duration ttl) {
        repository.deleteAllByUserIdAndPurpose(userId, purpose);
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        repository.save(new OneTimeToken(userId, purpose, sha256Hex(raw), clock.instant().plus(ttl)));
        return raw;
    }

    /**
     * Validates and consumes a token.
     *
     * @param rawToken token from the email link
     * @param purpose  the purpose the caller expects
     * @return id of the token's owner
     * @throws InvalidTokenException if unknown, wrong purpose, used or expired
     */
    @Transactional
    public UUID consume(final String rawToken, final TokenPurpose purpose) {
        OneTimeToken token = repository.findByTokenHash(sha256Hex(rawToken))
                .filter(t -> t.getPurpose() == purpose)
                .orElseThrow(() -> new InvalidTokenException(InvalidTokenException.Reason.INVALID));
        Instant now = clock.instant();
        if (token.isConsumed()) {
            throw new InvalidTokenException(InvalidTokenException.Reason.INVALID);
        }
        if (token.isExpired(now)) {
            throw new InvalidTokenException(InvalidTokenException.Reason.EXPIRED);
        }
        token.consume(now);
        return token.getUserId();
    }

    private static String sha256Hex(final String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256)
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
