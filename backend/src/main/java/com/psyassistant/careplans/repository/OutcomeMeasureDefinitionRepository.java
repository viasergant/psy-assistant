package com.psyassistant.careplans.repository;

import com.psyassistant.careplans.domain.OutcomeMeasureDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link OutcomeMeasureDefinition} reference data. */
@Repository
public interface OutcomeMeasureDefinitionRepository
        extends JpaRepository<OutcomeMeasureDefinition, UUID> {

    List<OutcomeMeasureDefinition> findByActiveTrueOrderBySortOrderAsc();

    Optional<OutcomeMeasureDefinition> findByCode(String code);
}
