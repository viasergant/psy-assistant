package com.psyassistant.common.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link DeploymentModeValidator}.
 *
 * <p>Verifies fail-fast behaviour when no deployment mode profile is active,
 * and no-throw behaviour for valid solo/clinic profile configurations.
 */
class DeploymentModeValidatorTest {

    private DeploymentModeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DeploymentModeValidator();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GenericApplicationContext contextWithProfiles(final String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.setEnvironment(env);
        return ctx;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void initializeThrowsWhenNoModeProfileActive() {
        // Arrange
        GenericApplicationContext ctx = contextWithProfiles("local");

        // Act
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> validator.initialize(ctx)
        );

        // Assert
        assertEquals(
            "Deployment mode not configured. "
            + "Start with --spring.profiles.active=solo or "
            + "--spring.profiles.active=clinic",
            ex.getMessage()
        );
    }

    @Test
    void initializeThrowsWhenNoProfilesSetAtAll() {
        // Arrange
        GenericApplicationContext ctx = contextWithProfiles();

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> validator.initialize(ctx));
    }

    @Test
    void initializeSucceedsWithSoloProfile() {
        // Arrange
        GenericApplicationContext ctx = contextWithProfiles("solo");

        // Act + Assert
        assertDoesNotThrow(() -> validator.initialize(ctx));
    }

    @Test
    void initializeSucceedsWithSoloAndLocalProfiles() {
        // Arrange
        GenericApplicationContext ctx = contextWithProfiles("solo", "local");

        // Act + Assert
        assertDoesNotThrow(() -> validator.initialize(ctx));
    }

    @Test
    void initializeSucceedsWithClinicProfile() {
        // Arrange
        GenericApplicationContext ctx = contextWithProfiles("clinic");

        // Act + Assert
        assertDoesNotThrow(() -> validator.initialize(ctx));
    }

    @Test
    void initializeSucceedsWithClinicAndLocalProfiles() {
        // Arrange
        GenericApplicationContext ctx = contextWithProfiles("clinic", "local");

        // Act + Assert
        assertDoesNotThrow(() -> validator.initialize(ctx));
    }

    @Test
    void initializeSucceedsWhenBothSoloAndClinicProfilesActive() {
        // Arrange — edge case: both mode profiles present; validator does not reject this
        GenericApplicationContext ctx = contextWithProfiles("solo", "clinic");

        // Act + Assert
        assertDoesNotThrow(() -> validator.initialize(ctx));
    }
}
