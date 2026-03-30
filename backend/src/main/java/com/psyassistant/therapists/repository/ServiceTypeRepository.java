package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.ServiceType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link ServiceType} reference data. */
@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, UUID> {
    Optional<ServiceType> findByNameIgnoreCase(String name);
}