package com.psyassistant.billing.catalog;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceCatalogPriceHistoryRepository
        extends JpaRepository<ServiceCatalogPriceHistory, UUID> {

    List<ServiceCatalogPriceHistory> findByServiceCatalogIdOrderByEffectiveFromDesc(UUID serviceId);

    /**
     * Returns the open (current) price record for a service without locking.
     * Used for read-only price display.
     */
    @Query("SELECT ph FROM ServiceCatalogPriceHistory ph "
           + "WHERE ph.serviceCatalog.id = :serviceId AND ph.effectiveTo IS NULL")
    Optional<ServiceCatalogPriceHistory> findOpenRecordReadOnly(@Param("serviceId") UUID serviceId);

    /**
     * Returns the open (current) price record for a service, acquiring a pessimistic write lock
     * to prevent two concurrent Finance users from creating duplicate open records.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ph FROM ServiceCatalogPriceHistory ph "
           + "WHERE ph.serviceCatalog.id = :serviceId AND ph.effectiveTo IS NULL")
    Optional<ServiceCatalogPriceHistory> findOpenRecordByServiceId(@Param("serviceId") UUID serviceId);

    /**
     * Finds the price history record effective on a given date (for CatalogPriceResolver).
     */
    @Query("SELECT ph FROM ServiceCatalogPriceHistory ph "
           + "WHERE ph.serviceCatalog.id = :serviceId "
           + "  AND ph.effectiveFrom <= :date "
           + "  AND (ph.effectiveTo IS NULL OR ph.effectiveTo >= :date) "
           + "ORDER BY ph.effectiveFrom DESC")
    List<ServiceCatalogPriceHistory> findEffectivePrice(
            @Param("serviceId") UUID serviceId,
            @Param("date") LocalDate date);
}
