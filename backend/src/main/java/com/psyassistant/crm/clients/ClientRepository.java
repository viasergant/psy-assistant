package com.psyassistant.crm.clients;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
