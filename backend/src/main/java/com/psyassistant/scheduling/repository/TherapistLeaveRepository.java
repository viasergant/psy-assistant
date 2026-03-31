package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.LeaveStatus;
import com.psyassistant.scheduling.domain.TherapistLeave;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TherapistLeave} entities.
 *
 * <p>Provides CRUD operations and custom queries for therapist leave periods.
 */
@Repository
public interface TherapistLeaveRepository extends JpaRepository<TherapistLeave, UUID> {

    /**
     * Finds all leave periods for a specific therapist with a given status.
     *
     * @param therapistProfileId therapist profile UUID
     * @param status leave status to filter by
     * @return list of leave periods with the specified status, empty if none found
     */
    List<TherapistLeave> findByTherapistProfileIdAndStatus(UUID therapistProfileId, LeaveStatus status);

    /**
     * Finds all leave periods for a specific therapist, ordered by start date descending.
     *
     * @param therapistProfileId therapist profile UUID
     * @return list of all leave periods, most recent first
     */
    List<TherapistLeave> findByTherapistProfileIdOrderByStartDateDesc(UUID therapistProfileId);

    /**
     * Finds all pending leave requests across all therapists, ordered by request date.
     *
     * @return list of pending leave requests, oldest first
     */
    List<TherapistLeave> findByStatusOrderByRequestedAtAsc(LeaveStatus status);

    /**
     * Finds all approved leave periods for a therapist that overlap with a given date range.
     *
     * <p>Overlap detection: (leave.startDate &lt;= endDate) AND (leave.endDate &gt;= startDate)
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate query start date
     * @param endDate query end date
     * @return list of approved leave periods overlapping the date range, empty if none found
     */
    @Query("""
        SELECT l FROM TherapistLeave l
        WHERE l.therapistProfileId = :therapistProfileId
        AND l.status = 'APPROVED'
        AND l.startDate <= :endDate
        AND l.endDate >= :startDate
        ORDER BY l.startDate ASC
        """)
    List<TherapistLeave> findApprovedLeaveOverlapping(
        @Param("therapistProfileId") UUID therapistProfileId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Counts active (non-cancelled, non-rejected) leave periods for a therapist
     * that overlap with a given date range.
     *
     * <p>Used for conflict detection when submitting new leave requests.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate query start date
     * @param endDate query end date
     * @return count of overlapping active leave periods
     */
    @Query("""
        SELECT COUNT(l) FROM TherapistLeave l
        WHERE l.therapistProfileId = :therapistProfileId
        AND l.status IN ('PENDING', 'APPROVED')
        AND l.startDate <= :endDate
        AND l.endDate >= :startDate
        """)
    long countOverlappingActiveLeave(
        @Param("therapistProfileId") UUID therapistProfileId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Deletes all leave periods for a specific therapist.
     *
     * @param therapistProfileId therapist profile UUID
     */
    void deleteByTherapistProfileId(UUID therapistProfileId);
}
