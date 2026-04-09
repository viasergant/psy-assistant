package com.psyassistant.billing.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {

    boolean existsByNameAndCategory(String name, String category);

    boolean existsByNameAndCategoryAndIdNot(String name, String category, UUID id);

    List<ServiceCatalog> findByStatus(ServiceStatus status);

    /**
     * Finds the active service catalog entry for a given session type.
     * Used by invoice generation to resolve the catalog base price.
     */
    Optional<ServiceCatalog> findFirstBySessionTypeIdAndStatus(UUID sessionTypeId, ServiceStatus status);
}
