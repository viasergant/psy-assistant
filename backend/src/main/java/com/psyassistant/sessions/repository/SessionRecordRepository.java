package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SessionRecord} persistence operations.
 *
 * <p>Provides standard CRUD operations plus custom queries for session record management.
 */
@Repository
public interface SessionRecordRepository extends JpaRepository<SessionRecord, UUID> {

    /**
     * Finds a session record by its associated appointment ID.
     *
     * @param appointmentId appointment UUID
     * @return optional session record, empty if no session exists for the appointment
     */
    Optional<SessionRecord> findByAppointmentId(UUID appointmentId);

    /**
     * Checks if a session record exists for a given appointment.
     *
     * @param appointmentId appointment UUID
     * @return true if a session record exists, false otherwise
     */
    boolean existsByAppointmentId(UUID appointmentId);

    /**
     * Finds all session records for a specific therapist, ordered by session date descending.
     *
     * @param therapistId therapist UUID
     * @return list of session records, empty if none found
     */
    List<SessionRecord> findByTherapistIdOrderBySessionDateDesc(UUID therapistId);

    /**
     * Query sessions with optional filters.
     *
     * <p>Uses COALESCE for date parameters to avoid PostgreSQL type inference issues.
     * When a date parameter is null, COALESCE returns the session date itself,
     * making the condition always true (e.g., sessionDate >= sessionDate).
     *
     * @param therapistId optional therapist UUID filter
     * @param startDate optional start date filter (inclusive)
     * @param endDate optional end date filter (inclusive)
     * @param status optional status filter
     * @return list of matching session records, ordered by session date descending
     */
    @Query("SELECT s FROM SessionRecord s WHERE "
            + "(:therapistId IS NULL OR s.therapistId = :therapistId) AND "
            + "s.sessionDate >= COALESCE(:startDate, s.sessionDate) AND "
            + "s.sessionDate <= COALESCE(:endDate, s.sessionDate) AND "
            + "(:status IS NULL OR s.status = :status) "
            + "ORDER BY s.sessionDate DESC, s.scheduledStartTime DESC")
    List<SessionRecord> findSessions(
            @Param("therapistId") UUID therapistId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") SessionStatus status
    );
}
