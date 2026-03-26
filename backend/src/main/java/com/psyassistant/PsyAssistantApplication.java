package com.psyassistant;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Psychological Assistance CRM backend application.
 */
@SpringBootApplication
public class PsyAssistantApplication {

    private static final String LEGACY_KYIV_TIME_ZONE = "Europe/Kiev";

    private static final String CANONICAL_KYIV_TIME_ZONE = "Europe/Kyiv";

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        normalizeLegacyKyivTimeZone();
        SpringApplication.run(PsyAssistantApplication.class, args);
    }

    static void normalizeLegacyKyivTimeZone() {
        final String currentTimeZoneId = TimeZone.getDefault().getID();

        if (LEGACY_KYIV_TIME_ZONE.equals(currentTimeZoneId)) {
            final TimeZone canonicalTimeZone = TimeZone.getTimeZone(CANONICAL_KYIV_TIME_ZONE);
            TimeZone.setDefault(canonicalTimeZone);
            System.setProperty("user.timezone", canonicalTimeZone.getID());
        }
    }
}
