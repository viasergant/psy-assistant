package com.psyassistant.notifications.telegram;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ClientTelegramLink}. The org filter applies whenever a tenant is bound.
 */
@Repository
public interface ClientTelegramLinkRepository extends JpaRepository<ClientTelegramLink, UUID> {

    Optional<ClientTelegramLink> findByClientId(UUID clientId);

    Optional<ClientTelegramLink> findByChatIdHash(String chatIdHash);
}
