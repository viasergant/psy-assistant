package com.psyassistant.billing.pkg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.billing.pkg.dto.CreatePackageDefinitionRequest;
import com.psyassistant.billing.pkg.dto.PackageDefinitionResponse;
import com.psyassistant.billing.pkg.dto.PackageInstanceResponse;
import com.psyassistant.billing.pkg.dto.SellPackageRequest;
import com.psyassistant.billing.pkg.dto.UpdatePackageDefinitionStatusRequest;
import com.psyassistant.notifications.BillingNotificationPort;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link PackageService}.
 *
 * <p>All tests use Mockito stubs. No Spring context is loaded.
 */
@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock
    private PrepaidPackageDefinitionRepository definitionRepository;

    @Mock
    private PrepaidPackageInstanceRepository instanceRepository;

    @Mock
    private PackageBalanceAuditRepository auditRepository;

    @Mock
    private BillingNotificationPort notifications;

    private PackageService service;

    private static final String ACTOR = "finance.staff";
    private static final UUID DEFINITION_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PackageService(definitionRepository, instanceRepository, auditRepository, notifications);
    }

    // =========================================================================
    // createDefinition
    // =========================================================================

    @Nested
    class CreateDefinition {

        @Test
        void savesDefinitionWhenNameIsUnique() {
            // Arrange
            CreatePackageDefinitionRequest request = new CreatePackageDefinitionRequest(
                    "10-Session Bundle", ServiceType.INDIVIDUAL_SESSION, 10, new BigDecimal("500.00"));
            when(definitionRepository.existsByName("10-Session Bundle")).thenReturn(false);

            PrepaidPackageDefinition saved = new PrepaidPackageDefinition(
                    "10-Session Bundle", ServiceType.INDIVIDUAL_SESSION, 10, new BigDecimal("500.00"));
            ReflectionTestUtils.setField(saved, "id", DEFINITION_ID);
            when(definitionRepository.save(any())).thenReturn(saved);

            // Act
            PackageDefinitionResponse response = service.createDefinition(request, ACTOR);

            // Assert
            assertThat(response.name()).isEqualTo("10-Session Bundle");
            assertThat(response.sessionQty()).isEqualTo(10);
            verify(definitionRepository).save(any(PrepaidPackageDefinition.class));
        }

        @Test
        void throwsWhenNameAlreadyExists() {
            // Arrange
            CreatePackageDefinitionRequest request = new CreatePackageDefinitionRequest(
                    "Duplicate Name", ServiceType.INDIVIDUAL_SESSION, 5, new BigDecimal("250.00"));
            when(definitionRepository.existsByName("Duplicate Name")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> service.createDefinition(request, ACTOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicate Name");
            verify(definitionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // updateDefinitionStatus
    // =========================================================================

    @Nested
    class UpdateDefinitionStatus {

        @Test
        void archivesDefinitionWhenFound() {
            // Arrange
            PrepaidPackageDefinition definition = new PrepaidPackageDefinition(
                    "Bundle", ServiceType.INDIVIDUAL_SESSION, 5, new BigDecimal("200.00"));
            ReflectionTestUtils.setField(definition, "id", DEFINITION_ID);
            when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(definition));

            UpdatePackageDefinitionStatusRequest request =
                    new UpdatePackageDefinitionStatusRequest(PackageDefinitionStatus.ARCHIVED);

            // Act
            PackageDefinitionResponse response = service.updateDefinitionStatus(DEFINITION_ID, request, ACTOR);

            // Assert
            assertThat(response.status()).isEqualTo(PackageDefinitionStatus.ARCHIVED);
        }

        @Test
        void throwsEntityNotFoundWhenDefinitionMissing() {
            // Arrange
            when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.updateDefinitionStatus(
                    DEFINITION_ID,
                    new UpdatePackageDefinitionStatusRequest(PackageDefinitionStatus.ARCHIVED),
                    ACTOR))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // =========================================================================
    // sellPackage
    // =========================================================================

    @Nested
    class SellPackage {

        private PrepaidPackageDefinition activeDefinition() {
            PrepaidPackageDefinition def = new PrepaidPackageDefinition(
                    "5-Session Pack", ServiceType.INDIVIDUAL_SESSION, 5, new BigDecimal("250.00"));
            ReflectionTestUtils.setField(def, "id", DEFINITION_ID);
            return def;
        }

        @Test
        void sellsPackageWithExpiryWhenValidityDaysProvided() {
            // Arrange
            PrepaidPackageDefinition def = activeDefinition();
            when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(def));

            SellPackageRequest request = new SellPackageRequest(
                    DEFINITION_ID, CLIENT_ID, Instant.parse("2026-05-27T10:00:00Z"), 30);

            PrepaidPackageInstance saved = new PrepaidPackageInstance(
                    def, CLIENT_ID, request.purchasedAt(), 5, LocalDate.of(2026, 6, 26));
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            when(instanceRepository.save(any())).thenReturn(saved);

            // Act
            PackageInstanceResponse response = service.sellPackage(request, ACTOR);

            // Assert
            assertThat(response.sessionsTotal()).isEqualTo(5);
            assertThat(response.expiresAt()).isEqualTo(LocalDate.of(2026, 6, 26));
            verify(instanceRepository).save(any(PrepaidPackageInstance.class));
        }

        @Test
        void sellsPackageWithNullExpiryWhenValidityDaysAbsent() {
            // Arrange
            PrepaidPackageDefinition def = activeDefinition();
            when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(def));

            SellPackageRequest request = new SellPackageRequest(
                    DEFINITION_ID, CLIENT_ID, Instant.now(), null);

            PrepaidPackageInstance saved = new PrepaidPackageInstance(
                    def, CLIENT_ID, request.purchasedAt(), 5, null);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            when(instanceRepository.save(any())).thenReturn(saved);

            // Act
            PackageInstanceResponse response = service.sellPackage(request, ACTOR);

            // Assert
            assertThat(response.expiresAt()).isNull();
        }

        @Test
        void throwsWhenDefinitionIsArchived() {
            // Arrange
            PrepaidPackageDefinition def = activeDefinition();
            def.setStatus(PackageDefinitionStatus.ARCHIVED);
            when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(def));

            SellPackageRequest request = new SellPackageRequest(
                    DEFINITION_ID, CLIENT_ID, Instant.now(), null);

            // Act & Assert
            assertThatThrownBy(() -> service.sellPackage(request, ACTOR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ARCHIVED");
        }
    }

    // =========================================================================
    // tryDeductSession
    // =========================================================================

    @Nested
    class TryDeductSession {

        private PrepaidPackageDefinition definition() {
            PrepaidPackageDefinition def = new PrepaidPackageDefinition(
                    "Pack", ServiceType.INDIVIDUAL_SESSION, 5, new BigDecimal("250.00"));
            ReflectionTestUtils.setField(def, "id", DEFINITION_ID);
            return def;
        }

        @Test
        void deductsSessionWhenEligiblePackageExists() {
            // Arrange
            UUID instanceId = UUID.randomUUID();
            PrepaidPackageInstance instance = new PrepaidPackageInstance(
                    definition(), CLIENT_ID, Instant.now(), 5, null);
            ReflectionTestUtils.setField(instance, "id", instanceId);

            when(instanceRepository.findFirstEligibleForDeduction(
                    any(), any(), any())).thenReturn(Optional.of(instance));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PackageDeductionResult result = service.tryDeductSession(
                    CLIENT_ID, ServiceType.INDIVIDUAL_SESSION, UUID.randomUUID(), UUID.randomUUID(), ACTOR);

            // Assert
            assertThat(result).isInstanceOf(PackageDeductionResult.Deducted.class);
            PackageDeductionResult.Deducted deducted = (PackageDeductionResult.Deducted) result;
            assertThat(deducted.balanceBefore()).isEqualTo(5);
            assertThat(deducted.balanceAfter()).isEqualTo(4);
            verify(auditRepository).save(any(PackageBalanceAudit.class));
        }

        @Test
        void returnsPackageExhaustedAfterLastSession() {
            // Arrange
            UUID instanceId = UUID.randomUUID();
            PrepaidPackageInstance instance = new PrepaidPackageInstance(
                    definition(), CLIENT_ID, Instant.now(), 1, null);
            ReflectionTestUtils.setField(instance, "id", instanceId);

            when(instanceRepository.findFirstEligibleForDeduction(
                    any(), any(), any())).thenReturn(Optional.of(instance));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            PackageDeductionResult result = service.tryDeductSession(
                    CLIENT_ID, ServiceType.INDIVIDUAL_SESSION, UUID.randomUUID(), UUID.randomUUID(), ACTOR);

            // Assert
            assertThat(result).isInstanceOf(PackageDeductionResult.PackageExhausted.class);
            assertThat(instance.getStatus()).isEqualTo(PackageInstanceStatus.EXHAUSTED);
            verify(notifications).notifyPackageExhausted(CLIENT_ID, instanceId);
        }

        @Test
        void returnsNoEligiblePackageWhenNoneFound() {
            // Arrange
            when(instanceRepository.findFirstEligibleForDeduction(
                    any(), any(), any())).thenReturn(Optional.empty());

            // Act
            PackageDeductionResult result = service.tryDeductSession(
                    CLIENT_ID, ServiceType.INDIVIDUAL_SESSION, UUID.randomUUID(), UUID.randomUUID(), ACTOR);

            // Assert
            assertThat(result).isInstanceOf(PackageDeductionResult.NoEligiblePackage.class);
            verify(auditRepository, never()).save(any());
        }
    }
}
