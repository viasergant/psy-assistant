package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.AttendanceAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link AttendanceAuditLog} — append-only, no deletes.
 */
@Repository
public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, UUID> {
}
