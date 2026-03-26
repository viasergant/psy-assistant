package com.psyassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Psychological Assistance CRM backend application.
 */
@SpringBootApplication
public class PsyAssistantApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(PsyAssistantApplication.class, args);
    }
}
