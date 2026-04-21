package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.SessionParticipantAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SessionParticipantAudit} append-only entries.
 */
@Repository
public interface SessionParticipantAuditRepository extends JpaRepository<SessionParticipantAudit, UUID> {

    /**
     * Finds all audit entries for a session, ordered by occurrence time ascending.
     *
     * @param sessionRecordId session record UUID
     * @return ordered list of audit entries
     */
    List<SessionParticipantAudit> findBySessionRecordIdOrderByOccurredAtAsc(UUID sessionRecordId);
}
