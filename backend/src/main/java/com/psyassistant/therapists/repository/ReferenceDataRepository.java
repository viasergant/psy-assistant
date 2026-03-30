package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.Language;
import com.psyassistant.therapists.domain.ServiceType;
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

/** Repository for {@link Language} reference data. */
@Repository
public interface LanguageRepository extends JpaRepository<Language, UUID> {
    Optional<Language> findByLanguageCode(String languageCode);
    Optional<Language> findByNameIgnoreCase(String name);
}

/** Repository for {@link ServiceType} reference data. */
@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, UUID> {
    Optional<ServiceType> findByNameIgnoreCase(String name);
}
