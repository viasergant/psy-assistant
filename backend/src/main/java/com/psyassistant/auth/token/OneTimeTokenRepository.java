package com.psyassistant.auth.token;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link OneTimeToken}.
 */
public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {

    /**
     * Finds a token by the SHA-256 hash of its raw value.
     *
     * @param tokenHash hex digest
     * @return the token, if any
     */
    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /**
     * Deletes all tokens of a user for one purpose.
     *
     * @param userId  token owner
     * @param purpose token purpose
     */
    void deleteAllByUserIdAndPurpose(UUID userId, TokenPurpose purpose);

    /**
     * Deletes every token owned by any of the given users.
     *
     * @param userIds owners
     */
    void deleteAllByUserIdIn(Collection<UUID> userIds);
}
