package com.psyassistant;

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
}
