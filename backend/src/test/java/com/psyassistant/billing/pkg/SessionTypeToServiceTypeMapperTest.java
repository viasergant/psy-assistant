package com.psyassistant.billing.pkg;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.billing.catalog.ServiceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link SessionTypeToServiceTypeMapper}.
 */
class SessionTypeToServiceTypeMapperTest {

    private final SessionTypeToServiceTypeMapper mapper = new SessionTypeToServiceTypeMapper();

    @ParameterizedTest(name = "code=''{0}'' -> {1}")
    @CsvSource({
        "IN_PERSON,  INDIVIDUAL_SESSION",
        "ONLINE,     ONLINE_SESSION",
        "INTAKE,     INTAKE_ASSESSMENT",
        "FOLLOW_UP,  FOLLOW_UP",
        "GROUP,      GROUP_SESSION"
    })
    void shouldMapKnownSessionTypeCodesToBillingServiceType(final String code, final ServiceType expected) {
        assertThat(mapper.resolve(code.strip())).hasValue(expected);
    }

    @Test
    void shouldReturnEmptyForUnknownCode() {
        assertThat(mapper.resolve("UNKNOWN_CODE")).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNull() {
        assertThat(mapper.resolve(null)).isEmpty();
    }
}
