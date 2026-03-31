package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.TherapistScheduleOverride;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TherapistScheduleOverride} entities.
 *
 * <p>Provides CRUD operations and custom queries for one-off schedule overrides.
 */
@Repository
public interface TherapistScheduleOverrideRepository extends JpaRepository<TherapistScheduleOverride, UUID> {

    /**
     * Finds all overrides for a specific therapist within a date range (inclusive).
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of overrides within the date range, empty if none found
     */
    @Query("""
        SELECT o FROM TherapistScheduleOverride o
        WHERE o.therapistProfileId = :therapistProfileId
        AND o.overrideDate BETWEEN :startDate AND :endDate
        ORDER BY o.overrideDate ASC
        """)
    List<TherapistScheduleOverride> findByTherapistProfileIdAndDateBetween(
        @Param("therapistProfileId") UUID therapistProfileId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Finds a specific override by therapist and date.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideDate the specific date
     * @return optional override, empty if not found
     */
    Optional<TherapistScheduleOverride> findByTherapistProfileIdAndOverrideDate(
        UUID therapistProfileId,
        LocalDate overrideDate
    );

    /**
     * Finds all overrides for a specific therapist.
     *
     * @param therapistProfileId therapist profile UUID
     * @return list of all overrides, ordered by date ascending
     */
    List<TherapistScheduleOverride> findByTherapistProfileIdOrderByOverrideDateAsc(UUID therapistProfileId);

    /**
     * Deletes all overrides for a specific therapist.
     *
     * @param therapistProfileId therapist profile UUID
     */
    void deleteByTherapistProfileId(UUID therapistProfileId);
}
