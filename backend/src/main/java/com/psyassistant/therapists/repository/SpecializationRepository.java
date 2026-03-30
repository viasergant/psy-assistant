package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.Specialization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link Specialization} reference data. */
@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
    Optional<Specialization> findByNameIgnoreCase(String name);
}