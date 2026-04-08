package com.psyassistant.careplans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.careplans.domain.AlertSeverity;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.OutcomeMeasureDefinition;
import com.psyassistant.careplans.domain.OutcomeMeasureEntry;
import com.psyassistant.careplans.dto.OutcomeMeasureChartDataResponse;
import com.psyassistant.careplans.dto.OutcomeMeasureEntryResponse;
import com.psyassistant.careplans.dto.RecordOutcomeMeasureRequest;
import com.psyassistant.careplans.exception.CarePlanNotActiveException;
import com.psyassistant.careplans.repository.CarePlanRepository;
import com.psyassistant.careplans.repository.OutcomeMeasureDefinitionRepository;
import com.psyassistant.careplans.repository.OutcomeMeasureEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link OutcomeMeasureService}.
 */
@ExtendWith(MockitoExtension.class)
class OutcomeMeasureServiceTest {

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private OutcomeMeasureDefinitionRepository definitionRepository;

    @Mock
    private OutcomeMeasureEntryRepository entryRepository;

    @Mock
    private CarePlanAuditService auditService;

    private OutcomeMeasureService service;

    private UUID clientId;
    private UUID therapistId;
    private UUID otherTherapistId;
    private UUID planId;
    private UUID definitionId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        therapistId = UUID.randomUUID();
        otherTherapistId = UUID.randomUUID();
        planId = UUID.randomUUID();
        definitionId = UUID.randomUUID();

        service = new OutcomeMeasureService(
                carePlanRepository,
                definitionRepository,
                entryRepository,
                auditService);
    }

    // -------------------------------------------------------------------------
    // recordEntry — success paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("recordEntry - saves entry when score is within range and no threshold breach")
    void recordEntryNoThresholdBreach() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(def));
        when(entryRepository.save(any(OutcomeMeasureEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 5, LocalDate.now().minusDays(1), null);

        final OutcomeMeasureEntryResponse response =
                service.recordEntry(planId, req, therapistId, "Dr. Smith");

        assertThat(response.score()).isEqualTo(5);
        assertThat(response.thresholdBreached()).isFalse();
        assertThat(response.alertLabel()).isNull();
        verify(entryRepository).save(any(OutcomeMeasureEntry.class));
        verify(auditService).recordOutcomeMeasureRecorded(any(), any(), any(), any());
    }

    @Test
    @DisplayName("recordEntry - sets thresholdBreached=true when score meets alert threshold")
    void recordEntryThresholdBreached() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(def));
        when(entryRepository.save(any(OutcomeMeasureEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 18, LocalDate.now().minusDays(1), null);

        final OutcomeMeasureEntryResponse response =
                service.recordEntry(planId, req, therapistId, "Dr. Smith");

        assertThat(response.score()).isEqualTo(18);
        assertThat(response.thresholdBreached()).isTrue();
        assertThat(response.alertLabel()).isEqualTo("Severe Depression Risk");
        assertThat(response.alertSeverity()).isEqualTo(AlertSeverity.ALERT);
    }

    @Test
    @DisplayName("recordEntry - sets thresholdBreached=true when score equals alert threshold exactly")
    void recordEntryThresholdBreachedAtExactBoundary() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(def));
        when(entryRepository.save(any(OutcomeMeasureEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 15, LocalDate.now().minusDays(1), null); // 15 = alertThreshold

        final OutcomeMeasureEntryResponse response =
                service.recordEntry(planId, req, therapistId, "Dr. Smith");

        assertThat(response.thresholdBreached()).isTrue();
    }

    // -------------------------------------------------------------------------
    // recordEntry — validation errors
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("recordEntry - throws 400 when score is below min range")
    void recordEntryThrowsWhenScoreBelowMin() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(def));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, -1, LocalDate.now().minusDays(1), null);

        assertThatThrownBy(() -> service.recordEntry(planId, req, therapistId, "Dr. Smith"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outside valid range");

        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordEntry - throws 400 when score is above max range")
    void recordEntryThrowsWhenScoreAboveMax() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(def));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 28, LocalDate.now().minusDays(1), null);

        assertThatThrownBy(() -> service.recordEntry(planId, req, therapistId, "Dr. Smith"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outside valid range");

        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordEntry - throws 400 when assessment date is in the future")
    void recordEntryThrowsForFutureDate() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(def));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 10, LocalDate.now().plusDays(1), null);

        assertThatThrownBy(() -> service.recordEntry(planId, req, therapistId, "Dr. Smith"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("future");

        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordEntry - throws CarePlanNotActiveException when plan is closed")
    void recordEntryThrowsWhenPlanNotActive() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        plan.close(therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 5, LocalDate.now().minusDays(1), null);

        assertThatThrownBy(() -> service.recordEntry(planId, req, therapistId, "Dr. Smith"))
                .isInstanceOf(CarePlanNotActiveException.class);

        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordEntry - throws AccessDeniedException for non-owner therapist")
    void recordEntryThrowsForNonOwner() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 5, LocalDate.now().minusDays(1), null);

        assertThatThrownBy(() ->
                service.recordEntry(planId, req, otherTherapistId, "Other"))
                .isInstanceOf(AccessDeniedException.class);

        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordEntry - throws EntityNotFoundException when definition not found")
    void recordEntryThrowsWhenDefinitionNotFound() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.empty());

        final RecordOutcomeMeasureRequest req = new RecordOutcomeMeasureRequest(
                definitionId, 5, LocalDate.now().minusDays(1), null);

        assertThatThrownBy(() -> service.recordEntry(planId, req, therapistId, "Dr. Smith"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getChartData
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getChartData - returns series ordered by assessmentDate ascending")
    void getChartDataReturnsSortedSeries() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findByCode("PHQ9")).thenReturn(Optional.of(def));

        final OutcomeMeasureEntry entry1 = entry(planId, definitionId, 5,
                LocalDate.of(2026, 1, 1), false);
        final OutcomeMeasureEntry entry2 = entry(planId, definitionId, 12,
                LocalDate.of(2026, 2, 1), false);
        final OutcomeMeasureEntry entry3 = entry(planId, definitionId, 17,
                LocalDate.of(2026, 3, 1), true);

        when(entryRepository.findByCarePlanIdAndMeasureDefinitionIdOrderByAssessmentDateAsc(
                planId, definitionId))
                .thenReturn(List.of(entry1, entry2, entry3));

        final OutcomeMeasureChartDataResponse response =
                service.getChartData(planId, "PHQ9", therapistId, false);

        assertThat(response.measureCode()).isEqualTo("PHQ9");
        assertThat(response.series()).hasSize(3);
        assertThat(response.series().get(0).score()).isEqualTo(5);
        assertThat(response.series().get(1).score()).isEqualTo(12);
        assertThat(response.series().get(2).score()).isEqualTo(17);
        assertThat(response.series().get(2).thresholdBreached()).isTrue();
    }

    @Test
    @DisplayName("getChartData - returns alertThreshold from definition")
    void getChartDataIncludesAlertThreshold() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final OutcomeMeasureDefinition def = phq9Definition(definitionId);
        when(definitionRepository.findByCode("PHQ9")).thenReturn(Optional.of(def));
        when(entryRepository.findByCarePlanIdAndMeasureDefinitionIdOrderByAssessmentDateAsc(
                planId, definitionId))
                .thenReturn(List.of());

        final OutcomeMeasureChartDataResponse response =
                service.getChartData(planId, "PHQ9", therapistId, false);

        assertThat(response.alertThreshold()).isEqualTo(15);
    }

    @Test
    @DisplayName("getChartData - throws EntityNotFoundException when measure code is unknown")
    void getChartDataThrowsForUnknownCode() {
        final CarePlan plan = activePlan(planId, clientId, therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(definitionRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getChartData(planId, "UNKNOWN", therapistId, false))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CarePlan activePlan(final UUID pId, final UUID cId, final UUID tId) {
        final CarePlan plan = new CarePlan(cId, tId, "Test Plan", null);
        ReflectionTestUtils.setField(plan, "id", pId);
        return plan;
    }

    /**
     * Builds a mocked {@link OutcomeMeasureDefinition} for PHQ-9 (range 0-27, threshold 15).
     */
    private OutcomeMeasureDefinition phq9Definition(final UUID defId) {
        final OutcomeMeasureDefinition def = mock(OutcomeMeasureDefinition.class);
        lenient().when(def.getId()).thenReturn(defId);
        lenient().when(def.getCode()).thenReturn("PHQ9");
        lenient().when(def.getDisplayName()).thenReturn("PHQ-9");
        lenient().when(def.getMinScore()).thenReturn(0);
        lenient().when(def.getMaxScore()).thenReturn(27);
        lenient().when(def.getAlertThreshold()).thenReturn(15);
        lenient().when(def.getAlertLabel()).thenReturn("Severe Depression Risk");
        lenient().when(def.getAlertSeverity()).thenReturn(AlertSeverity.ALERT);
        return def;
    }

    private OutcomeMeasureEntry entry(
            final UUID pId, final UUID defId, final int score,
            final LocalDate date, final boolean breached) {
        final OutcomeMeasureEntry e = new OutcomeMeasureEntry(
                pId, defId, score, date, null,
                therapistId, "Dr. Smith",
                breached,
                breached ? "Severe Depression Risk" : null,
                breached ? AlertSeverity.ALERT : null);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        return e;
    }
}
