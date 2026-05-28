package com.psyassistant.billing.pkg;

import com.psyassistant.billing.catalog.ServiceType;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps scheduling {@link com.psyassistant.scheduling.domain.SessionType} codes
 * to billing {@link ServiceType} values for package deduction.
 *
 * <p>{@code IN_PERSON} maps to {@code INDIVIDUAL_SESSION} and {@code ONLINE} maps to
 * {@code ONLINE_SESSION} so that packages can be sold per delivery modality. Unknown
 * or null codes return {@link Optional#empty()} so
 * that callers can decide whether to skip deduction gracefully.
 */
@Component
public class SessionTypeToServiceTypeMapper {

    private static final Map<String, ServiceType> MAPPING = Map.of(
            "IN_PERSON",  ServiceType.INDIVIDUAL_SESSION,
            "ONLINE",     ServiceType.ONLINE_SESSION,
            "INTAKE",     ServiceType.INTAKE_ASSESSMENT,
            "FOLLOW_UP",  ServiceType.FOLLOW_UP,
            "GROUP",      ServiceType.GROUP_SESSION
    );

    /**
     * Resolves the billing {@link ServiceType} for the given scheduling session type code.
     *
     * @param sessionTypeCode the scheduling session type code (e.g. {@code "IN_PERSON"}, {@code "GROUP"})
     * @return the corresponding {@link ServiceType}, or empty if no mapping is defined
     */
    public Optional<ServiceType> resolve(final String sessionTypeCode) {
        if (sessionTypeCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(MAPPING.get(sessionTypeCode));
    }
}
