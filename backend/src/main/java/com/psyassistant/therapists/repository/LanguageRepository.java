package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.Language;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link Language} reference data. */
@Repository
public interface LanguageRepository extends JpaRepository<Language, UUID> {
    Optional<Language> findByLanguageCode(String languageCode);

    Optional<Language> findByNameIgnoreCase(String name);
}