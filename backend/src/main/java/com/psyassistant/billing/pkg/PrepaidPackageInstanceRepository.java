package com.psyassistant.billing.pkg;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for {@link PrepaidPackageInstance}. */
public interface PrepaidPackageInstanceRepository extends JpaRepository<PrepaidPackageInstance, UUID> {

    List<PrepaidPackageInstance> findByClientIdOrderByPurchasedAtAsc(UUID clientId);

    /**
     * FIFO-selects the oldest active package matching the given client and service type.
     * Applies a pessimistic write lock to prevent concurrent double-deduction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT p FROM PrepaidPackageInstance p
           JOIN p.definition d
           WHERE p.clientId = :clientId
             AND d.serviceType = :serviceType
             AND p.status = 'ACTIVE'
             AND p.sessionsRemaining > 0
             AND (p.expiresAt IS NULL OR p.expiresAt >= :today)
           ORDER BY p.purchasedAt ASC
           """)
    Optional<PrepaidPackageInstance> findFirstEligibleForDeduction(
            @Param("clientId") UUID clientId,
            @Param("serviceType") com.psyassistant.billing.catalog.ServiceType serviceType,
            @Param("today") LocalDate today);

    /**
     * Finds active packages whose expiry date is in the past (for the expiry job).
     */
    @Query("""
           SELECT p FROM PrepaidPackageInstance p
           WHERE p.status = 'ACTIVE'
             AND p.expiresAt IS NOT NULL
             AND p.expiresAt < :asOf
           """)
    List<PrepaidPackageInstance> findOverdue(@Param("asOf") LocalDate asOf);
}
