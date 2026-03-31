package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.SessionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SessionType} lookup data.
 *
 * <p>Provides access to active session types for appointment booking dropdowns.
 */
@Repository
public interface SessionTypeRepository extends JpaRepository<SessionType, UUID> {

    /**
     * Finds a session type by its unique code.
     *
     * @param code session type code (e.g., "IN_PERSON", "ONLINE")
     * @return session type if found
     */
    Optional<SessionType> findByCode(String code);

    /**
     * Finds all active session types (for UI dropdowns).
     *
     * @return list of active session types, ordered by name
     */
    List<SessionType> findByIsActiveTrueOrderByName();
}
