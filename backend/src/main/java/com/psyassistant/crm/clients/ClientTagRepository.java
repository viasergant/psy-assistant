package com.psyassistant.crm.clients;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for client tags.
 */
public interface ClientTagRepository extends JpaRepository<ClientTag, UUID> {

    /**
     * Returns all tags for one client.
     */
    List<ClientTag> findAllByClientId(UUID clientId);

    /**
     * Deletes all tags for one client.
     */
    void deleteAllByClientId(UUID clientId);
}
