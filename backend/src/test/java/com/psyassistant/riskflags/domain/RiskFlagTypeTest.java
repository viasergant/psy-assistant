package com.psyassistant.riskflags.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RiskFlagType} domain entity.
 */
@DisplayName("RiskFlagType")
class RiskFlagTypeTest {

    @Test
    @DisplayName("new RiskFlagType is active by default")
    void newFlagTypeIsActiveByDefault() {
        // Arrange + Act
        RiskFlagType flagType = new RiskFlagType("Self-Harm Risk", (short) 1);

        // Assert
        assertThat(flagType.isActive()).isTrue();
    }

    @Test
    @DisplayName("constructor retains name")
    void constructorRetainsName() {
        // Arrange + Act
        RiskFlagType flagType = new RiskFlagType("Crisis History", (short) 2);

        // Assert
        assertThat(flagType.getName()).isEqualTo("Crisis History");
    }

    @Test
    @DisplayName("constructor retains display order")
    void constructorRetainsDisplayOrder() {
        // Arrange + Act
        RiskFlagType flagType = new RiskFlagType("Safeguarding Concern", (short) 3);

        // Assert
        assertThat(flagType.getDisplayOrder()).isEqualTo((short) 3);
    }

    @Test
    @DisplayName("deactivate sets active to false")
    void deactivateSetsActiveToFalse() {
        // Arrange
        RiskFlagType flagType = new RiskFlagType("Suicidal Ideation", (short) 5);

        // Act
        flagType.deactivate();

        // Assert
        assertThat(flagType.isActive()).isFalse();
    }

    @Test
    @DisplayName("id is null before persistence")
    void idIsNullBeforePersistence() {
        // Arrange + Act
        RiskFlagType flagType = new RiskFlagType("Domestic Abuse Concern", (short) 4);

        // Assert
        assertThat(flagType.getId()).isNull();
    }

    @Test
    @DisplayName("createdAt is null before persistence")
    void createdAtIsNullBeforePersistence() {
        // Arrange + Act
        RiskFlagType flagType = new RiskFlagType("Self-Harm Risk", (short) 1);

        // Assert
        assertThat(flagType.getCreatedAt()).isNull();
    }
}
