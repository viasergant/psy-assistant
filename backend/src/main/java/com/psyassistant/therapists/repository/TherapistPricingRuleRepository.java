package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.TherapistPricingRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link TherapistPricingRule} persistence operations. */
@Repository
public interface TherapistPricingRuleRepository extends JpaRepository<TherapistPricingRule, UUID> {

    /**
     * Finds all pricing rules for a given therapist profile.
     *
     * @param therapistProfileId the therapist profile ID
     * @return list of pricing rules ordered by effective date (DESC)
     */
    List<TherapistPricingRule> findByTherapistProfileIdOrderByEffectiveFromDesc(UUID therapistProfileId);
}
