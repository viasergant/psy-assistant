package com.psyassistant.therapists.dto;

import com.psyassistant.therapists.domain.TherapistPricingRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response representation of a therapist pricing rule.
 *
 * <p>Includes a nested {@link SessionTypeRef} with the canonical session type details
 * so callers can display the session type code and name without a second request.
 */
public record PricingRuleResponse(
        UUID id,
        SessionTypeRef sessionType,
        BigDecimal rate,
        String currency,
        LocalDate effectiveFrom
) {

    /**
     * Minimal projection of the canonical session type lookup.
     *
     * @param id   session type UUID
     * @param code e.g. "IN_PERSON", "INTAKE"
     * @param name e.g. "In-Person Session"
     */
    public record SessionTypeRef(UUID id, String code, String name) { }

    /** Converts an entity to the response DTO. */
    public static PricingRuleResponse from(final TherapistPricingRule rule) {
        var st = rule.getSessionType();
        return new PricingRuleResponse(
                rule.getId(),
                new SessionTypeRef(st.getId(), st.getCode(), st.getName()),
                rule.getRate(),
                rule.getCurrency(),
                rule.getEffectiveFrom()
        );
    }
}
