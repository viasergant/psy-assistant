package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.TherapistCredential;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link TherapistCredential} persistence operations. */
@Repository
public interface TherapistCredentialRepository extends JpaRepository<TherapistCredential, UUID> {

    /**
     * Finds all credentials for a given therapist profile.
     *
     * @param therapistProfileId the therapist profile ID
     * @return list of credentials
     */
    List<TherapistCredential> findByTherapistProfileId(UUID therapistProfileId);
}
