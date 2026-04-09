package com.psyassistant.billing.pkg;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link PrepaidPackageDefinition}. */
public interface PrepaidPackageDefinitionRepository extends JpaRepository<PrepaidPackageDefinition, UUID> {

    List<PrepaidPackageDefinition> findByStatusOrderByName(PackageDefinitionStatus status);

    boolean existsByName(String name);
}
