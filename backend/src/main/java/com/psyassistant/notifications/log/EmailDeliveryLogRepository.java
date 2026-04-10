package com.psyassistant.notifications.log;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link EmailDeliveryLog}.
 */
@Repository
public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, UUID> {

    /**
     * Fetches the next batch of deliverable log entries using SKIP LOCKED to avoid
     * contention between concurrent poller instances.
     *
     * @param now       current time used to filter entries whose retry delay has elapsed
     * @param batchSize maximum number of entries to return
     * @return list of {@link EmailDeliveryLog} entries ready for dispatch
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT l FROM EmailDeliveryLog l
            WHERE l.status IN ('PENDING', 'RETRY')
              AND (l.nextRetryAt IS NULL OR l.nextRetryAt <= :now)
            ORDER BY l.createdAt ASC
            LIMIT :batchSize
            """)
    List<EmailDeliveryLog> findNextBatch(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize);

    /**
     * Looks up a delivery log entry by the provider-assigned message identifier.
     * Used for bounce webhook correlation.
     *
     * @param providerMessageId the message ID returned by the SMTP provider
     * @return the matching log entry, or empty if not found
     */
    Optional<EmailDeliveryLog> findByProviderMessageId(String providerMessageId);
}
