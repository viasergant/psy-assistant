package com.psyassistant.careplans.repository;

import com.psyassistant.careplans.domain.OutcomeMeasureEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Append-only repository for {@link OutcomeMeasureEntry} records. */
@Repository
public interface OutcomeMeasureEntryRepository extends JpaRepository<OutcomeMeasureEntry, UUID> {

    List<OutcomeMeasureEntry> findByCarePlanIdAndMeasureDefinitionIdOrderByAssessmentDateAsc(
            UUID carePlanId, UUID measureDefinitionId);

    @Query("""
        SELECT e FROM OutcomeMeasureEntry e
        JOIN OutcomeMeasureDefinition d ON d.id = e.measureDefinitionId
        WHERE e.carePlanId = :carePlanId
          AND (:measureCode IS NULL OR d.code = :measureCode)
          AND (:from IS NULL OR e.assessmentDate >= :from)
          AND (:to IS NULL OR e.assessmentDate <= :to)
        ORDER BY e.assessmentDate DESC
        """)
    Page<OutcomeMeasureEntry> findFiltered(
            @Param("carePlanId") UUID carePlanId,
            @Param("measureCode") String measureCode,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
