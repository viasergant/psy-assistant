package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.GroupSessionAttendance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link GroupSessionAttendance} persistence operations.
 */
@Repository
public interface GroupSessionAttendanceRepository extends JpaRepository<GroupSessionAttendance, UUID> {

    /**
     * Finds all per-client attendance records for a group session.
     *
     * @param sessionRecordId session record UUID
     * @return list of per-client attendance records
     */
    List<GroupSessionAttendance> findBySessionRecordId(UUID sessionRecordId);

    /**
     * Finds the attendance record for a specific client within a group session.
     *
     * @param sessionRecordId session record UUID
     * @param clientId        client UUID
     * @return optional attendance record
     */
    Optional<GroupSessionAttendance> findBySessionRecordIdAndClientId(UUID sessionRecordId, UUID clientId);
}
