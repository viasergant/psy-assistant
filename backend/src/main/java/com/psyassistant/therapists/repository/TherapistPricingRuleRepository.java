package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.TherapistPricingRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link TherapistPricingRule} persistence operations. */
@Repository
public interface TherapistPricingRuleRepository extends JpaRepository<TherapistPricingRule, UUID> {

    /**
     * Finds all pricing rules for a given therapist profile, newest first.
     *
     * @param therapistProfileId the therapist profile ID
     * @return list of pricing rules ordered by effective date (DESC)
     */
    List<TherapistPricingRule> findByTherapistProfileIdOrderByEffectiveFromDesc(UUID therapistProfileId);

    /**
     * Finds pricing rules for a specific therapist and session type, newest first.
     * Used for FK-based price resolution in invoice generation.
     *
     * @param therapistProfileId the therapist profile ID
     * @param sessionTypeId      the canonical session type ID
     * @return list of matching pricing rules ordered by effective date (DESC)
     */
    List<TherapistPricingRule> findByTherapistProfileIdAndSessionTypeIdOrderByEffectiveFromDesc(
            UUID therapistProfileId, UUID sessionTypeId);

    /**
     * Checks whether a pricing rule already exists for the given combination.
     * Used to enforce the unique constraint from the application layer before
     * a DB-level duplicate-key error.
     *
     * @param therapistProfileId the therapist profile ID
     * @param sessionTypeId      the canonical session type ID
     * @return an {@link Optional} with the first matching rule if present
     */
    Optional<TherapistPricingRule> findFirstByTherapistProfileIdAndSessionTypeId(
            UUID therapistProfileId, UUID sessionTypeId);
}
