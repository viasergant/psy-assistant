package com.psyassistant.notifications.telegram;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TelegramLinkToken}.
 */
@Repository
public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, UUID> {

    Optional<TelegramLinkToken> findByTokenHash(String tokenHash);
}
