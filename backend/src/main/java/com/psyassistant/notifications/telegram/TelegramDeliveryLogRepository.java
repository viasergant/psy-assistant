package com.psyassistant.notifications.telegram;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TelegramDeliveryLog}.
 */
@Repository
public interface TelegramDeliveryLogRepository extends JpaRepository<TelegramDeliveryLog, UUID> {

    boolean existsByDedupKey(String dedupKey);

    /**
     * Claims the next deliverable batch; SKIP LOCKED keeps concurrent instances apart.
     *
     * @param now       current time
     * @param batchSize maximum rows
     * @return rows ready to send
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT l FROM TelegramDeliveryLog l
            WHERE l.status IN ('PENDING', 'RETRY')
              AND (l.nextRetryAt IS NULL OR l.nextRetryAt <= :now)
            ORDER BY l.createdAt ASC
            LIMIT :batchSize
            """)
    List<TelegramDeliveryLog> findNextBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);

    /**
     * Cancels undelivered messages of a user, for example after unlinking.
     *
     * @param userId the user whose pending messages are cancelled
     * @return number of rows updated
     */
    @Modifying
    @Query("""
            UPDATE TelegramDeliveryLog l SET l.status = 'CANCELLED'
            WHERE l.userId = :userId AND l.status IN ('PENDING', 'RETRY')
            """)
    int cancelPendingForUser(@Param("userId") UUID userId);
}
