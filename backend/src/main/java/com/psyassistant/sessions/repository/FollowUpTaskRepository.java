package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.FollowUpTask;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link FollowUpTask}.
 *
 * <p>The UNIQUE constraint on {@code session_record_id} ensures idempotency — only
 * one follow-up task can exist per session. Concurrent inserts will result in a
 * {@link org.springframework.dao.DataIntegrityViolationException} for the duplicate,
 * which should be caught and silently ignored.
 */
@Repository
public interface FollowUpTaskRepository extends JpaRepository<FollowUpTask, UUID> {

    /**
     * Checks whether a follow-up task already exists for the given session.
     *
     * @param sessionRecordId session record UUID
     * @return true if a task already exists
     */
    boolean existsBySessionRecordId(UUID sessionRecordId);
}
