package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.SessionRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
