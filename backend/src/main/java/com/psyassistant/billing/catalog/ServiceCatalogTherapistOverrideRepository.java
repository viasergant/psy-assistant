package com.psyassistant.billing.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogTherapistOverrideRepository
        extends JpaRepository<ServiceCatalogTherapistOverride, UUID> {

    Optional<ServiceCatalogTherapistOverride> findByServiceCatalogIdAndTherapistId(
            UUID serviceId, UUID therapistId);

    List<ServiceCatalogTherapistOverride> findByServiceCatalogId(UUID serviceId);
}
