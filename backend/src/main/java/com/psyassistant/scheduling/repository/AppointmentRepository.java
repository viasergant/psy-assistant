package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Appointment} entity.
 *
 * <p>Provides CRUD operations and conflict detection queries using PostgreSQL tstzrange.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Finds all appointments for a therapist on a specific date range.
     *
     * @param therapistProfileId therapist UUID
     * @param startDate start of range (inclusive)
     * @param endDate end of range (exclusive)
     * @return list of appointments
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.therapistProfileId = :therapistProfileId
        AND a.startTime >= :startDate
        AND a.startTime < :endDate
        AND a.status != 'CANCELLED'
        ORDER BY a.startTime ASC
        """)
    List<Appointment> findByTherapistAndDateRange(
            @Param("therapistProfileId") UUID therapistProfileId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate
    );

    /**
     * Finds all appointments for a client, ordered by start time descending.
     *
     * @param clientId client UUID
     * @return list of appointments (most recent first)
     */
    List<Appointment> findByClientIdOrderByStartTimeDesc(UUID clientId);

    /**
     * <strong>CRITICAL CONFLICT DETECTION QUERY</strong>
     *
     * <p>Uses PostgreSQL tstzrange with GIST index for O(log n) overlap detection.
     *
     * <p>Finds all non-cancelled appointments for a therapist that overlap with the given time range.
     * The GIST index ({@code idx_appointment_conflict_detection}) makes this query extremely fast
     * even with 500+ appointments per therapist.
     *
     * <p><strong>How it works</strong>:
     * <ol>
     *     <li>Creates tstzrange from appointment start_time to calculated end_time</li>
     *     <li>Creates tstzrange from input start to calculated end</li>
     *     <li>Uses {@code &&} overlap operator (optimized by GIST index)</li>
     *     <li>Excludes CANCELLED appointments via partial index</li>
     * </ol>
     *
     * <p><strong>Example</strong>:
     * <pre>{@code
     * // Check if 10:00-11:00 conflicts with existing appointments
     * ZonedDateTime start = ZonedDateTime.of(2026, 3, 31, 10, 0, 0, 0, ZoneId.of("America/New_York"));
     * int duration = 60;
     * List<Appointment> conflicts = repo.findConflictingAppointments(therapistId, start, duration);
     * if (!conflicts.isEmpty()) {
     *     // Conflict detected!
     * }
     * }</pre>
     *
     * @param therapistProfileId therapist UUID
     * @param startTime proposed appointment start time (with timezone)
     * @param durationMinutes proposed appointment duration in minutes
     * @return list of conflicting appointments (empty if no conflicts)
     */
    @Query(value = """
        SELECT * FROM appointment a
        WHERE a.therapist_profile_id = :therapistProfileId
        AND a.status != 'CANCELLED'
        AND tstzrange(
            a.start_time,
            a.start_time + (a.duration_minutes || ' minutes')::INTERVAL
        ) && tstzrange(
            CAST(:startTime AS timestamptz),
            CAST(:startTime AS timestamptz) + (:durationMinutes || ' minutes')::INTERVAL
        )
        ORDER BY a.start_time
        """, nativeQuery = true)
    List<Appointment> findConflictingAppointments(
            @Param("therapistProfileId") UUID therapistProfileId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("durationMinutes") Integer durationMinutes
    );

    /**
     * Finds conflicting appointments excluding a specific appointment (for reschedule checks).
     *
     * <p>Same as {@link #findConflictingAppointments} but excludes the appointment being rescheduled.
     *
     * @param therapistProfileId therapist UUID
     * @param startTime proposed new start time
     * @param durationMinutes proposed duration
     * @param excludeAppointmentId appointment ID to exclude from conflict check
     * @return list of conflicting appointments
     */
    @Query(value = """
        SELECT * FROM appointment a
        WHERE a.therapist_profile_id = :therapistProfileId
        AND a.status != 'CANCELLED'
        AND a.id != :excludeAppointmentId
        AND tstzrange(
            a.start_time,
            a.start_time + (a.duration_minutes || ' minutes')::INTERVAL
        ) && tstzrange(
            CAST(:startTime AS timestamptz),
            CAST(:startTime AS timestamptz) + (:durationMinutes || ' minutes')::INTERVAL
        )
        ORDER BY a.start_time
        """, nativeQuery = true)
    List<Appointment> findConflictingAppointmentsExcluding(
            @Param("therapistProfileId") UUID therapistProfileId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("durationMinutes") Integer durationMinutes,
            @Param("excludeAppointmentId") UUID excludeAppointmentId
    );

    /**
     * Counts appointments by therapist and status (for analytics).
     *
     * @param therapistProfileId therapist UUID
     * @param status appointment status
     * @return count
     */
    long countByTherapistProfileIdAndStatus(UUID therapistProfileId, AppointmentStatus status);
}
