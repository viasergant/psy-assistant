package com.psyassistant.crm.clients;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Returns all distinct tag values across all clients, sorted alphabetically.
     */
    @Query("SELECT DISTINCT ct.tag FROM ClientTag ct ORDER BY ct.tag")
    List<String> findAllDistinctTags();
}
