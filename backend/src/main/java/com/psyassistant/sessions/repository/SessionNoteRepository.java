package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.SessionNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SessionNote} persistence.
 */
@Repository
public interface SessionNoteRepository extends JpaRepository<SessionNote, UUID> {

    /**
     * Returns all notes for a session record, ordered by creation time ascending.
     */
    List<SessionNote> findBySessionRecordIdOrderByCreatedAtAsc(UUID sessionRecordId);

    /**
     * Returns notes for a session record authored by a specific principal.
     */
    List<SessionNote> findBySessionRecordIdAndAuthorIdOrderByCreatedAtAsc(
            UUID sessionRecordId, String authorId);
}
