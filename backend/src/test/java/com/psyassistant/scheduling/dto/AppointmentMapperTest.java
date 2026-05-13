package com.psyassistant.scheduling.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.psyassistant.riskflags.domain.ClientRiskFlag;
import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import com.psyassistant.riskflags.domain.RiskFlagType;
import com.psyassistant.riskflags.repository.ClientRiskFlagRepository;
import com.psyassistant.riskflags.repository.RiskFlagTypeRepository;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.SessionType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AppointmentMapper}.
 *
 * <p>Covers the {@code activeRiskFlagTypes} enrichment added in PA-27 Increment 6.
 * All tests use Mockito only — no Spring context, no infrastructure dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentMapper tests")
class AppointmentMapperTest {

    @Mock
    private ClientRiskFlagRepository clientRiskFlagRepository;

    @Mock
    private RiskFlagTypeRepository riskFlagTypeRepository;

    private AppointmentMapper mapper;

    private UUID clientId;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        mapper = new AppointmentMapper(clientRiskFlagRepository, riskFlagTypeRepository);

        clientId = UUID.randomUUID();

        final SessionType sessionType = new SessionType("INDIVIDUAL", "Individual Session", "One-on-one therapy");
        ReflectionTestUtils.setField(sessionType, "id", UUID.randomUUID());

        appointment = new Appointment(
                UUID.randomUUID(),
                clientId,
                sessionType,
                ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1),
                50,
                "UTC"
        );
        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(appointment, "status", AppointmentStatus.SCHEDULED);
        ReflectionTestUtils.setField(appointment, "version", 0L);
    }

    // =========================================================================
    // toResponse — slim list view (no repository queries)
    // =========================================================================

    @Test
    @DisplayName("toResponse returns null when appointment is null")
    void toResponseReturnsNullWhenAppointmentIsNull() {
        // Act
        final AppointmentResponse result = mapper.toResponse(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toResponse returns empty activeRiskFlagTypes without querying repositories")
    void toResponseReturnsEmptyRiskFlagTypesWithoutRepositoryQueries() {
        // Act — no mock stubs; Mockito strict mode would fail if any repository is called
        final AppointmentResponse response = mapper.toResponse(appointment);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.activeRiskFlagTypes()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("toResponse maps base appointment fields correctly")
    void toResponseMapsBaseFieldsCorrectly() {
        // Act
        final AppointmentResponse response = mapper.toResponse(appointment);

        // Assert
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.sessionType()).isNotNull();
        assertThat(response.sessionType().code()).isEqualTo("INDIVIDUAL");
        assertThat(response.activeRiskFlagTypes()).isEmpty();
    }

    // =========================================================================
    // toDetailResponse — null guard
    // =========================================================================

    @Test
    @DisplayName("toDetailResponse returns null when appointment is null")
    void toDetailResponseReturnsNullWhenAppointmentIsNull() {
        // Act
        final AppointmentResponse result = mapper.toDetailResponse(null);

        // Assert
        assertThat(result).isNull();
    }

    // =========================================================================
    // toDetailResponse — no active flags
    // =========================================================================

    @Test
    @DisplayName("toDetailResponse includes empty activeRiskFlagTypes when client has no active flags")
    void toDetailResponseIncludesEmptyRiskFlagTypesWhenClientHasNoActiveFlags() {
        // Arrange
        when(clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of());

        // Act
        final AppointmentResponse response = mapper.toDetailResponse(appointment);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.activeRiskFlagTypes()).isNotNull().isEmpty();
    }

    // =========================================================================
    // toDetailResponse — active flags present
    // =========================================================================

    @Test
    @DisplayName("toDetailResponse includes flag type names when client has active flags")
    void toDetailResponseIncludesFlagTypeNamesWhenClientHasActiveFlags() {
        // Arrange
        final UUID flagTypeId = UUID.randomUUID();

        final ClientRiskFlag activeFlag = new ClientRiskFlag(
                clientId, flagTypeId, null,
                LocalDate.now().plusDays(7),
                UUID.randomUUID()
        );

        final RiskFlagType flagType = new RiskFlagType("Self-Harm Risk", (short) 1);
        ReflectionTestUtils.setField(flagType, "id", flagTypeId);

        when(clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of(activeFlag));
        when(riskFlagTypeRepository.findAllById(Set.of(flagTypeId)))
                .thenReturn(List.of(flagType));

        // Act
        final AppointmentResponse response = mapper.toDetailResponse(appointment);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.activeRiskFlagTypes())
                .containsExactly("Self-Harm Risk");
    }

    @Test
    @DisplayName("toDetailResponse deduplicates flag type names when multiple flags share a type")
    void toDetailResponseDeduplicatesFlagTypeNamesWhenMultipleFlagsShareType() {
        // Arrange
        final UUID flagTypeId = UUID.randomUUID();

        final ClientRiskFlag flag1 = new ClientRiskFlag(
                clientId, flagTypeId, "note 1",
                LocalDate.now().plusDays(7),
                UUID.randomUUID()
        );
        final ClientRiskFlag flag2 = new ClientRiskFlag(
                clientId, flagTypeId, "note 2",
                LocalDate.now().plusDays(14),
                UUID.randomUUID()
        );

        final RiskFlagType flagType = new RiskFlagType("Crisis History", (short) 2);
        ReflectionTestUtils.setField(flagType, "id", flagTypeId);

        when(clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of(flag1, flag2));
        when(riskFlagTypeRepository.findAllById(Set.of(flagTypeId)))
                .thenReturn(List.of(flagType));

        // Act
        final AppointmentResponse response = mapper.toDetailResponse(appointment);

        // Assert
        assertThat(response.activeRiskFlagTypes())
                .hasSize(1)
                .containsExactly("Crisis History");
    }

    @Test
    @DisplayName("toDetailResponse includes multiple distinct flag type names when client has different active flags")
    void toDetailResponseIncludesMultipleNamesWhenClientHasMultipleDistinctActiveFlagTypes() {
        // Arrange
        final UUID flagTypeId1 = UUID.randomUUID();
        final UUID flagTypeId2 = UUID.randomUUID();

        final ClientRiskFlag flag1 = new ClientRiskFlag(
                clientId, flagTypeId1, null,
                LocalDate.now().plusDays(7),
                UUID.randomUUID()
        );
        final ClientRiskFlag flag2 = new ClientRiskFlag(
                clientId, flagTypeId2, null,
                LocalDate.now().plusDays(14),
                UUID.randomUUID()
        );

        final RiskFlagType type1 = new RiskFlagType("Self-Harm Risk", (short) 1);
        ReflectionTestUtils.setField(type1, "id", flagTypeId1);
        final RiskFlagType type2 = new RiskFlagType("Suicidal Ideation", (short) 5);
        ReflectionTestUtils.setField(type2, "id", flagTypeId2);

        when(clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of(flag1, flag2));
        when(riskFlagTypeRepository.findAllById(Set.of(flagTypeId1, flagTypeId2)))
                .thenReturn(List.of(type1, type2));

        // Act
        final AppointmentResponse response = mapper.toDetailResponse(appointment);

        // Assert
        assertThat(response.activeRiskFlagTypes())
                .hasSize(2)
                .containsExactlyInAnyOrder("Self-Harm Risk", "Suicidal Ideation");
    }

    // =========================================================================
    // toDetailResponse — orphaned flag type (soft-deleted type, flag still active)
    // =========================================================================

    @Test
    @DisplayName("toDetailResponse silently omits flag types not found in the type repository")
    void toDetailResponseOmitsNameWhenFlagTypeNotFoundInRepository() {
        // Arrange — flag references a type that is not returned by the repository
        // (e.g., type was hard-deleted directly in DB, which should never happen but is handled)
        final UUID flagTypeId = UUID.randomUUID();
        final ClientRiskFlag activeFlag = new ClientRiskFlag(
                clientId, flagTypeId, null,
                LocalDate.now().plusDays(7),
                UUID.randomUUID()
        );

        when(clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of(activeFlag));
        // Repository returns empty — type was deleted
        when(riskFlagTypeRepository.findAllById(Set.of(flagTypeId)))
                .thenReturn(List.of());

        // Act
        final AppointmentResponse response = mapper.toDetailResponse(appointment);

        // Assert — result is empty rather than throwing NPE
        assertThat(response.activeRiskFlagTypes()).isEmpty();
    }

    // =========================================================================
    // toDetailResponse — base fields are preserved correctly
    // =========================================================================

    @Test
    @DisplayName("toDetailResponse maps appointment base fields correctly alongside risk flags")
    void toDetailResponseMapsBaseFieldsCorrectlyAlongWithRiskFlags() {
        // Arrange
        when(clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE))
                .thenReturn(List.of());

        // Act
        final AppointmentResponse response = mapper.toDetailResponse(appointment);

        // Assert
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.sessionType()).isNotNull();
        assertThat(response.sessionType().code()).isEqualTo("INDIVIDUAL");
        assertThat(response.activeRiskFlagTypes()).isEmpty();
    }
}
