package com.psyassistant.riskflags.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ClientRiskFlag} domain entity.
 */
@DisplayName("ClientRiskFlag")
class ClientRiskFlagTest {

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID FLAG_TYPE_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final LocalDate REVIEW_DATE = LocalDate.now().plusDays(30);

    @Test
    @DisplayName("constructor sets ACTIVE status and all provided fields")
    void constructorSetsActiveStatusAndAllProvidedFields() {
        // Arrange + Act
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "clinical note", REVIEW_DATE, CREATED_BY);

        // Assert
        assertThat(flag.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(flag.getFlagTypeId()).isEqualTo(FLAG_TYPE_ID);
        assertThat(flag.getClinicalNote()).isEqualTo("clinical note");
        assertThat(flag.getReviewDate()).isEqualTo(REVIEW_DATE);
        assertThat(flag.getCreatedByUserId()).isEqualTo(CREATED_BY);
        assertThat(flag.getStatus()).isEqualTo(ClientRiskFlagStatus.ACTIVE);
    }

    @Test
    @DisplayName("constructor accepts null clinical note")
    void constructorAcceptsNullClinicalNote() {
        // Arrange + Act
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                null, REVIEW_DATE, CREATED_BY);

        // Assert
        assertThat(flag.getClinicalNote()).isNull();
    }

    @Test
    @DisplayName("resolution fields are null when flag is active")
    void resolutionFieldsAreNullWhenFlagIsActive() {
        // Arrange + Act
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "note", REVIEW_DATE, CREATED_BY);

        // Assert
        assertThat(flag.getResolvedByUserId()).isNull();
        assertThat(flag.getResolvedAt()).isNull();
        assertThat(flag.getResolutionNote()).isNull();
    }

    @Test
    @DisplayName("resolve sets status to RESOLVED")
    void resolveSetsStatusToResolved() {
        // Arrange
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "note", REVIEW_DATE, CREATED_BY);
        UUID resolver = UUID.randomUUID();

        // Act
        flag.resolve(resolver, "Resolution explanation");

        // Assert
        assertThat(flag.getStatus()).isEqualTo(ClientRiskFlagStatus.RESOLVED);
    }

    @Test
    @DisplayName("resolve sets resolvedByUserId")
    void resolveSetsResolvedByUserId() {
        // Arrange
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "note", REVIEW_DATE, CREATED_BY);
        UUID resolver = UUID.randomUUID();

        // Act
        flag.resolve(resolver, "Resolution explanation");

        // Assert
        assertThat(flag.getResolvedByUserId()).isEqualTo(resolver);
    }

    @Test
    @DisplayName("resolve sets resolution note")
    void resolveSetsResolutionNote() {
        // Arrange
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "note", REVIEW_DATE, CREATED_BY);

        // Act
        flag.resolve(UUID.randomUUID(), "Client discharged safely");

        // Assert
        assertThat(flag.getResolutionNote()).isEqualTo("Client discharged safely");
    }

    @Test
    @DisplayName("resolve sets resolvedAt to current time")
    void resolveSetsResolvedAtToCurrentTime() {
        // Arrange
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "note", REVIEW_DATE, CREATED_BY);
        Instant before = Instant.now();

        // Act
        flag.resolve(UUID.randomUUID(), "note");
        Instant after = Instant.now();

        // Assert
        assertThat(flag.getResolvedAt()).isNotNull();
        assertThat(flag.getResolvedAt()).isAfterOrEqualTo(before);
        assertThat(flag.getResolvedAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("id and createdAt are null before persistence")
    void idAndCreatedAtAreNullBeforePersistence() {
        // Arrange + Act
        ClientRiskFlag flag = new ClientRiskFlag(CLIENT_ID, FLAG_TYPE_ID,
                "note", REVIEW_DATE, CREATED_BY);

        // Assert
        assertThat(flag.getId()).isNull();
        assertThat(flag.getCreatedAt()).isNull();
    }
}
