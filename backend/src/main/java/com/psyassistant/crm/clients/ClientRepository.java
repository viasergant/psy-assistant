package com.psyassistant.crm.clients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Searches clients using PostgreSQL full-text search with prefix matching.
     * Matches against name, email, phone, and client code fields.
     * Supports partial keyword matching (e.g., "lead" matches "lead1", "leading").
     * Uses GIN index on search_vector for sub-500ms performance on 10k+ records.
     *
     * @param searchQuery text query (case-insensitive, supports partial matching)
     * @param limit maximum number of results to return
     * @return list of matching clients ordered by relevance
     */
    /**
     * Returns a paginated, filtered list of clients.
     * All parameters are optional — null values are ignored.
     * Tag filter is OR-based: client must have at least one of the provided tags.
     * Therapist filter is a direct UUID equality check on assigned_therapist_id.
     *
     * @param q              optional text filter (ILIKE on name, email, phone, code, tags)
     * @param tags           optional list of tags (OR semantics); pass null or empty to skip
     * @param therapistId    optional therapist UUID; pass null to skip
     * @param pageable       pagination and sort
     * @return page of matching clients
     */
    @Query(
        value = """
            SELECT DISTINCT c FROM Client c
            LEFT JOIN ClientTag ct ON ct.client.id = c.id
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.clientCode) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(ct.tag) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:#{#tags == null || #tags.isEmpty()} = true
                OR ct.tag IN :tags)
            AND (:therapistId IS NULL OR c.assignedTherapistId = :therapistId)
            """,
        countQuery = """
            SELECT COUNT(DISTINCT c) FROM Client c
            LEFT JOIN ClientTag ct ON ct.client.id = c.id
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.clientCode) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(ct.tag) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:#{#tags == null || #tags.isEmpty()} = true
                OR ct.tag IN :tags)
            AND (:therapistId IS NULL OR c.assignedTherapistId = :therapistId)
            """
    )
    Page<Client> findByFilter(
            @Param("q") String q,
            @Param("tags") List<String> tags,
            @Param("therapistId") UUID therapistId,
            Pageable pageable);

    @Query(value = """
        SELECT c.*
        FROM clients c
        WHERE c.id IN (
            SELECT DISTINCT c2.id
            FROM clients c2
            LEFT JOIN client_tags ct ON c2.id = ct.client_id
            WHERE c2.search_vector @@ to_tsquery('simple', :searchQuery || ':*')
               OR c2.full_name ILIKE '%' || :searchQuery || '%'
               OR c2.email ILIKE '%' || :searchQuery || '%'
               OR c2.phone ILIKE '%' || :searchQuery || '%'
               OR c2.client_code ILIKE '%' || :searchQuery || '%'
               OR ct.tag ILIKE '%' || :searchQuery || '%'
        )
        ORDER BY 
            CASE 
                WHEN c.search_vector @@ to_tsquery('simple', :searchQuery || ':*') 
                THEN ts_rank(c.search_vector, to_tsquery('simple', :searchQuery || ':*'))
                ELSE 0
            END DESC,
            c.full_name
        LIMIT :limit
        """, nativeQuery = true)
    List<Client> searchClients(@Param("searchQuery") String searchQuery, @Param("limit") int limit);
}
