package com.psyassistant.therapists.service;

import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import com.psyassistant.therapists.domain.TherapistPricingRule;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.dto.CreatePricingRuleRequest;
import com.psyassistant.therapists.dto.PricingRuleResponse;
import com.psyassistant.therapists.repository.TherapistPricingRuleRepository;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing per-therapist session-type pricing rules.
 *
 * <p>RBAC rules:
 * <ul>
 *   <li>Write operations: Finance Staff, System Administrator</li>
 *   <li>Read operations: Finance Staff, System Administrator, Supervisor, Therapist</li>
 * </ul>
 */
@Service
public class TherapistPricingRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(TherapistPricingRuleService.class);

    private final TherapistPricingRuleRepository ruleRepository;
    private final TherapistProfileRepository profileRepository;
    private final SessionTypeRepository sessionTypeRepository;

    public TherapistPricingRuleService(
            final TherapistPricingRuleRepository ruleRepository,
            final TherapistProfileRepository profileRepository,
            final SessionTypeRepository sessionTypeRepository) {
        this.ruleRepository = ruleRepository;
        this.profileRepository = profileRepository;
        this.sessionTypeRepository = sessionTypeRepository;
    }

    /**
     * Returns all pricing rules for the given therapist, newest effective date first.
     *
     * @param therapistId therapist profile UUID
     * @return list of pricing rule responses
     */
    @PreAuthorize("hasAuthority('READ_PRICING_RULES')")
    @Transactional(readOnly = true)
    public List<PricingRuleResponse> listRules(final UUID therapistId) {
        validateTherapistExists(therapistId);
        return ruleRepository
                .findByTherapistProfileIdOrderByEffectiveFromDesc(therapistId)
                .stream()
                .map(PricingRuleResponse::from)
                .toList();
    }

    /**
     * Creates a new pricing rule for the given therapist.
     *
     * <p>Validates that the session type exists and is active. Throws
     * {@link EntityNotFoundException} if the therapist or session type is not found,
     * and {@link IllegalArgumentException} if the session type is inactive.
     *
     * @param therapistId the therapist profile UUID
     * @param request     the create request
     * @param actorName   principal name for audit
     * @return the created pricing rule response
     */
    @PreAuthorize("hasAuthority('MANAGE_PRICING_RULES')")
    @Transactional
    public PricingRuleResponse createRule(final UUID therapistId,
                                          final CreatePricingRuleRequest request,
                                          final String actorName) {
        TherapistProfile profile = profileRepository.findById(therapistId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Therapist profile not found: " + therapistId));

        SessionType sessionType = sessionTypeRepository.findById(request.sessionTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Session type not found: " + request.sessionTypeId()));

        if (!Boolean.TRUE.equals(sessionType.getIsActive())) {
            throw new IllegalArgumentException(
                    "Session type is inactive: " + request.sessionTypeId());
        }

        TherapistPricingRule rule = new TherapistPricingRule(
                profile,
                sessionType,
                request.rate(),
                request.currency(),
                request.effectiveFrom()
        );
        rule.setUpdatedByUser(actorName);

        TherapistPricingRule saved = ruleRepository.save(rule);
        LOG.info("Pricing rule created for therapist {} session-type {} by {}",
                therapistId, sessionType.getCode(), actorName);
        return PricingRuleResponse.from(saved);
    }

    private void validateTherapistExists(final UUID therapistId) {
        if (!profileRepository.existsById(therapistId)) {
            throw new EntityNotFoundException("Therapist profile not found: " + therapistId);
        }
    }
}
