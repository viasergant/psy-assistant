package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.SessionNoteVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SessionNoteVersion} — immutable version history records.
 */
@Repository
public interface SessionNoteVersionRepository extends JpaRepository<SessionNoteVersion, UUID> {

    /**
     * Returns all versions for a note ordered chronologically (oldest first).
     */
    List<SessionNoteVersion> findByNoteIdOrderByVersionNumberAsc(UUID noteId);

    /**
     * Returns the highest assigned version number for a note (used when appending new versions).
     */
    int countByNoteId(UUID noteId);
}
