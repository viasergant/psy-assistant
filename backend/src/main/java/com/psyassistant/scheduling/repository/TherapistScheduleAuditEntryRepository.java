package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.TherapistScheduleAuditEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TherapistScheduleAuditEntry} entities.
 *
 * <p>Provides queries for retrieving schedule change audit history.
 */
@Repository
public interface TherapistScheduleAuditEntryRepository extends JpaRepository<TherapistScheduleAuditEntry, UUID> {

    /**
     * Finds recent audit entries for a specific therapist, ordered by creation time descending.
     *
     * @param therapistProfileId therapist profile UUID
     * @param pageable pagination parameters
     * @return list of audit entries, most recent first
     */
    @Query("""
        SELECT e FROM TherapistScheduleAuditEntry e
        WHERE e.therapistProfileId = :therapistProfileId
        ORDER BY e.createdAt DESC
        """)
    List<TherapistScheduleAuditEntry> findByTherapistProfileIdOrderByCreatedAtDesc(
        @Param("therapistProfileId") UUID therapistProfileId,
        Pageable pageable
    );

    /**
     * Finds audit entries for a specific schedule entity.
     *
     * @param entityType entity type (RECURRING_SCHEDULE, OVERRIDE, LEAVE)
     * @param entityId entity UUID
     * @return list of audit entries for that entity, ordered by creation time
     */
    List<TherapistScheduleAuditEntry> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
        String entityType,
        UUID entityId
    );

    /**
     * Finds all audit entries for a specific therapist and entity type.
     *
     * @param therapistProfileId therapist profile UUID
     * @param entityType entity type (RECURRING_SCHEDULE, OVERRIDE, LEAVE)
     * @return list of audit entries matching the criteria
     */
    List<TherapistScheduleAuditEntry> findByTherapistProfileIdAndEntityTypeOrderByCreatedAtDesc(
        UUID therapistProfileId,
        String entityType
    );
}
