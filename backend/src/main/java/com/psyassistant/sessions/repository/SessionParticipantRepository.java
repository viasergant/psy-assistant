package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.SessionParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SessionParticipant} persistence operations.
 */
@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    /**
     * Finds all participants (active and removed) for a session.
     *
     * @param sessionRecordId session record UUID
     * @return list of all session participants
     */
    List<SessionParticipant> findBySessionRecordId(UUID sessionRecordId);

    /**
     * Finds only active (not removed) participants for a session.
     *
     * @param sessionRecordId session record UUID
     * @return list of active participants
     */
    @Query("SELECT p FROM SessionParticipant p WHERE p.sessionRecordId = :sessionId AND p.removedAt IS NULL")
    List<SessionParticipant> findActiveBySessionRecordId(@Param("sessionId") UUID sessionRecordId);

    /**
     * Counts active participants for a session. Used to enforce the ≤ 20 cap at application layer.
     *
     * @param sessionRecordId session record UUID
     * @return count of active participants
     */
    @Query("SELECT COUNT(p) FROM SessionParticipant p WHERE p.sessionRecordId = :sessionId AND p.removedAt IS NULL")
    long countActiveBySessionRecordId(@Param("sessionId") UUID sessionRecordId);

    /**
     * Finds a specific client's participant record in a session.
     *
     * @param sessionRecordId session record UUID
     * @param clientId        client UUID
     * @return optional participant record
     */
    Optional<SessionParticipant> findBySessionRecordIdAndClientId(UUID sessionRecordId, UUID clientId);

    /**
     * Finds all group sessions in which a client is an active participant.
     *
     * @param clientId client UUID
     * @return list of session participant records for the client
     */
    @Query("SELECT p FROM SessionParticipant p WHERE p.clientId = :clientId AND p.removedAt IS NULL")
    List<SessionParticipant> findActiveByClientId(@Param("clientId") UUID clientId);
}
