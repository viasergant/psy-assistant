package com.psyassistant.riskflags.repository;

import com.psyassistant.riskflags.domain.RiskFlagType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for {@link RiskFlagType}. */
@Repository
public interface RiskFlagTypeRepository extends JpaRepository<RiskFlagType, UUID> {

    /**
     * Returns all active flag types ordered by their display position.
     * Used to populate the flag creation form.
     *
     * @return active flag types in ascending display order
     */
    List<RiskFlagType> findAllByActiveTrueOrderByDisplayOrderAsc();
}
