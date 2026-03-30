package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.TherapistProfileAuditEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link TherapistProfileAuditEntry} persistence operations. */
@Repository
public interface TherapistProfileAuditEntryRepository extends JpaRepository<TherapistProfileAuditEntry, UUID> {

    /**
     * Finds all audit entries for a given therapist profile, ordered by creation time (most recent first).
     *
     * @param therapistProfileId the therapist profile ID
     * @return list of audit entries
     */
    List<TherapistProfileAuditEntry> findByTherapistProfileIdOrderByCreatedAtDesc(UUID therapistProfileId);
}
