package com.psyassistant;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the Spring application context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class PsyAssistantApplicationTest {

    /**
     * Verifies that the application context starts successfully.
     *
     * <p>If any bean definition is invalid or a required dependency is missing,
     * this test will fail with a context-loading exception.
     */
    @Test
    void contextLoads() {
        // No assertions needed: the test passes if the context loads without throwing
    }

    @Test
    void normalizeLegacyKyivTimeZoneReplacesDeprecatedZoneId() {
        final TimeZone originalTimeZone = TimeZone.getDefault();
        final String originalUserTimeZone = System.getProperty("user.timezone");

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
            System.setProperty("user.timezone", "Europe/Kiev");

            PsyAssistantApplication.normalizeLegacyKyivTimeZone();

            assertThat(TimeZone.getDefault().getID()).isEqualTo("Europe/Kyiv");
            assertThat(System.getProperty("user.timezone")).isEqualTo("Europe/Kyiv");
        } finally {
            TimeZone.setDefault(originalTimeZone);
            restoreUserTimeZone(originalUserTimeZone);
        }
    }

    @Test
    void normalizeLegacyKyivTimeZoneKeepsOtherTimeZonesUntouched() {
        final TimeZone originalTimeZone = TimeZone.getDefault();
        final String originalUserTimeZone = System.getProperty("user.timezone");

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            System.setProperty("user.timezone", "UTC");

            PsyAssistantApplication.normalizeLegacyKyivTimeZone();

            assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
            assertThat(System.getProperty("user.timezone")).isEqualTo("UTC");
        } finally {
            TimeZone.setDefault(originalTimeZone);
            restoreUserTimeZone(originalUserTimeZone);
        }
    }

    private static void restoreUserTimeZone(final String originalUserTimeZone) {
        if (originalUserTimeZone == null) {
            System.clearProperty("user.timezone");
            return;
        }

        System.setProperty("user.timezone", originalUserTimeZone);
    }
}
