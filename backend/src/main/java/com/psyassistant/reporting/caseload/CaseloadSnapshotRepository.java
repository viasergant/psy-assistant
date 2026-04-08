package com.psyassistant.reporting.caseload;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link CaseloadSnapshot} persistence operations.
 */
@Repository
public interface CaseloadSnapshotRepository extends JpaRepository<CaseloadSnapshot, UUID> {

    /**
     * Returns the latest snapshots for all therapists, optionally filtered by
     * a set of allowed therapist IDs and/or specialization IDs.
     *
     * <p>Joins to {@code therapist_profile} for the name and optional specialization filter.
     *
     * @param therapistIds     set of allowed therapist profile IDs; if empty, returns nothing
     * @param specializationIds optional list of specialization IDs; null means no filter
     * @param snapshotDate     snapshot date to read; null means use the latest available date
     * @param pageable         pagination parameters
     * @return page of snapshot results
     */
    @Query(value = """
            SELECT cs.*
            FROM caseload_snapshot cs
            JOIN therapist_profile tp ON tp.id = cs.therapist_profile_id
            WHERE cs.therapist_profile_id IN (:therapistIds)
              AND (:snapshotDate IS NULL OR cs.snapshot_date = :snapshotDate)
              AND cs.snapshot_date = (
                  SELECT MAX(cs2.snapshot_date)
                  FROM caseload_snapshot cs2
                  WHERE cs2.therapist_profile_id = cs.therapist_profile_id
                    AND (:snapshotDate IS NULL OR cs2.snapshot_date = :snapshotDate)
              )
              AND (:#{#specializationIds == null || #specializationIds.isEmpty()} = true
                   OR EXISTS (
                       SELECT 1 FROM therapist_specialization ts
                       WHERE ts.therapist_profile_id = tp.id
                         AND ts.specialization_id IN (:specializationIds)
                   ))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT cs.therapist_profile_id)
            FROM caseload_snapshot cs
            JOIN therapist_profile tp ON tp.id = cs.therapist_profile_id
            WHERE cs.therapist_profile_id IN (:therapistIds)
              AND (:snapshotDate IS NULL OR cs.snapshot_date = :snapshotDate)
              AND cs.snapshot_date = (
                  SELECT MAX(cs2.snapshot_date)
                  FROM caseload_snapshot cs2
                  WHERE cs2.therapist_profile_id = cs.therapist_profile_id
                    AND (:snapshotDate IS NULL OR cs2.snapshot_date = :snapshotDate)
              )
              AND (:#{#specializationIds == null || #specializationIds.isEmpty()} = true
                   OR EXISTS (
                       SELECT 1 FROM therapist_specialization ts
                       WHERE ts.therapist_profile_id = tp.id
                         AND ts.specialization_id IN (:specializationIds)
                   ))
            """,
            nativeQuery = true)
    Page<CaseloadSnapshot> findLatestByTherapistIds(
            @Param("therapistIds") Set<UUID> therapistIds,
            @Param("specializationIds") List<UUID> specializationIds,
            @Param("snapshotDate") LocalDate snapshotDate,
            Pageable pageable);

    /**
     * Returns the name for a given therapist profile ID.
     *
     * @param therapistProfileId therapist profile UUID
     * @return the therapist name, or null if not found
     */
    @Query(value = "SELECT name FROM therapist_profile WHERE id = :id", nativeQuery = true)
    String findTherapistNameById(@Param("id") UUID therapistProfileId);

    /**
     * Returns all therapist profile IDs for active therapist profiles.
     *
     * @return set of active therapist profile UUIDs
     */
    @Query(value = "SELECT id FROM therapist_profile WHERE active = true", nativeQuery = true)
    Set<UUID> findAllActiveTherapistProfileIds();

    /**
     * Returns the names for multiple therapist profile IDs, as a list of {@code [id, name]} pairs.
     *
     * @param therapistIds set of therapist profile IDs
     * @return list of Object arrays, each containing [id (UUID), name (String)]
     */
    @Query(value = "SELECT id, name FROM therapist_profile WHERE id IN (:therapistIds)",
            nativeQuery = true)
    List<Object[]> findTherapistNamesById(@Param("therapistIds") Set<UUID> therapistIds);
}
