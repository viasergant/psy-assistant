package com.psyassistant.riskflags.repository;

import com.psyassistant.riskflags.domain.ClientRiskFlag;
import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for {@link ClientRiskFlag}. */
@Repository
public interface ClientRiskFlagRepository extends JpaRepository<ClientRiskFlag, UUID> {

    /**
     * Returns all flags for a client filtered by status.
     *
     * @param clientId client to query
     * @param status   status filter (ACTIVE or RESOLVED)
     * @return matching flags in no guaranteed order
     */
    List<ClientRiskFlag> findAllByClientIdAndStatus(UUID clientId, ClientRiskFlagStatus status);

    /**
     * Returns the full flag history for a client, newest first.
     * Intended for supervisor/administrator use only.
     *
     * @param clientId client to query
     * @return all flags ordered by creation timestamp descending
     */
    List<ClientRiskFlag> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);
}
