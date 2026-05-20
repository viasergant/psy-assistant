package com.psyassistant.riskflags.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RiskFlagAuditLog} domain entity.
 */
@DisplayName("RiskFlagAuditLog")
class RiskFlagAuditLogTest {

    private static final UUID FLAG_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    @DisplayName("constructor retains all provided fields")
    void constructorRetainsAllProvidedFields() {
        // Arrange + Act
        RiskFlagAuditLog entry = new RiskFlagAuditLog(
                FLAG_ID, CLIENT_ID, ACTOR_ID, "Dr. Smith",
                RiskFlagAuditActionType.FLAG_CREATED, "Self-Harm Risk", "ACTIVE");

        // Assert
        assertThat(entry.getFlagId()).isEqualTo(FLAG_ID);
        assertThat(entry.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(entry.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(entry.getActorName()).isEqualTo("Dr. Smith");
        assertThat(entry.getActionType()).isEqualTo(RiskFlagAuditActionType.FLAG_CREATED);
        assertThat(entry.getFlagTypeName()).isEqualTo("Self-Harm Risk");
        assertThat(entry.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("id is null before persistence")
    void idIsNullBeforePersistence() {
        // Arrange + Act
        RiskFlagAuditLog entry = new RiskFlagAuditLog(
                FLAG_ID, CLIENT_ID, ACTOR_ID, "Dr. Smith",
                RiskFlagAuditActionType.FLAG_RESOLVED, "Crisis History", "RESOLVED");

        // Assert
        assertThat(entry.getId()).isNull();
    }

    @Test
    @DisplayName("actionTimestamp is null before persistence — set by DB default")
    void actionTimestampIsNullBeforePersistence() {
        // Arrange + Act
        RiskFlagAuditLog entry = new RiskFlagAuditLog(
                FLAG_ID, CLIENT_ID, ACTOR_ID, "Dr. Smith",
                RiskFlagAuditActionType.FLAG_UPDATED, "Safeguarding Concern", "ACTIVE");

        // Assert — timestamp is set by DB DEFAULT NOW(), not the constructor
        assertThat(entry.getActionTimestamp()).isNull();
    }

    @Test
    @DisplayName("all action types are constructable")
    void allActionTypesAreConstructable() {
        // Arrange + Act + Assert — verify all enum values are constructable
        for (RiskFlagAuditActionType actionType : RiskFlagAuditActionType.values()) {
            RiskFlagAuditLog entry = new RiskFlagAuditLog(
                    FLAG_ID, CLIENT_ID, ACTOR_ID, "Actor",
                    actionType, "Some Flag Type", "ACTIVE");
            assertThat(entry.getActionType()).isEqualTo(actionType);
        }
    }
}
