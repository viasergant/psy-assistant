package com.psyassistant.crm.clients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link Client} entities.
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    /**
     * Finds a client by the UUID of the originating lead.
     *
     * @param sourceLeadId the UUID of the lead that was converted
     * @return the matching client, or empty if none
     */
    Optional<Client> findBySourceLeadId(UUID sourceLeadId);

    /**
     * Searches clients using PostgreSQL full-text search.
     * Matches against name, email, phone, and client code fields.
     * Uses GIN index on search_vector for sub-500ms performance on 10k+ records.
     *
     * @param searchQuery text query (case-insensitive, supports partial matching)
     * @param limit maximum number of results to return
     * @return list of matching clients ordered by relevance
     */
    @Query(value = """
        SELECT DISTINCT c.*
        FROM clients c
        LEFT JOIN client_tags ct ON c.id = ct.client_id
        WHERE c.search_vector @@ plainto_tsquery('simple', :searchQuery)
           OR ct.tag ILIKE '%' || :searchQuery || '%'
        ORDER BY ts_rank(c.search_vector, plainto_tsquery('simple', :searchQuery)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Client> searchClients(@Param("searchQuery") String searchQuery, @Param("limit") int limit);
}
