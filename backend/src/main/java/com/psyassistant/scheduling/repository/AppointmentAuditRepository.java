package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.AppointmentAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link AppointmentAudit} immutable audit trail.
 *
 * <p><strong>IMPORTANT</strong>: This repository should ONLY be used for INSERT operations.
 * Updates and deletes are prohibited to maintain audit integrity.
 */
@Repository
public interface AppointmentAuditRepository extends JpaRepository<AppointmentAudit, UUID> {

    /**
     * Retrieves all audit entries for a specific appointment, ordered by timestamp descending.
     *
     * @param appointmentId appointment UUID
     * @return list of audit entries (most recent first)
     */
    List<AppointmentAudit> findByAppointmentIdOrderByActionTimestampDesc(UUID appointmentId);

    /**
     * Retrieves recent audit entries for a specific user.
     *
     * @param actorUserId user UUID
     * @return list of audit entries (most recent first)
     */
    List<AppointmentAudit> findByActorUserIdOrderByActionTimestampDesc(UUID actorUserId);
}
