package com.psyassistant.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse.ConflictDetail;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link BatchConflictDetectionService}.
 *
 * <p>Verifies that batch detection correctly aggregates per-slot results
 * from the underlying {@link ConflictDetectionService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchConflictDetectionService Tests")
class BatchConflictDetectionServiceTest {

    @Mock
    private ConflictDetectionService conflictDetectionService;

    @InjectMocks
    private BatchConflictDetectionService batchConflictDetectionService;

    private static final ZoneId NYC = ZoneId.of("America/New_York");
    private static final UUID THERAPIST_ID = UUID.randomUUID();
    private static final int DURATION = 60;

    private List<ZonedDateTime> threeSlots;

    @BeforeEach
    void setUp() {
        final ZonedDateTime anchor = ZonedDateTime.of(2026, 4, 1, 10, 0, 0, 0, NYC);
        threeSlots = List.of(
                anchor,
                anchor.plusWeeks(1),
                anchor.plusWeeks(2)
        );
    }

    @Test
    @DisplayName("No conflicts: returns empty map")
    void noConflictsReturnsEmptyMap() {
        when(conflictDetectionService.findConflictingAppointments(any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        final Map<Integer, ConflictDetail> result =
                batchConflictDetectionService.detectBatch(THERAPIST_ID, threeSlots, DURATION);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("One conflicting slot: returns map with correct index")
    void oneConflictReturnsSingleEntry() {
        final Appointment conflicting = buildAppointment();
        // index 1 conflicts, others clean
        final ZonedDateTime slot0 = threeSlots.get(0);
        final ZonedDateTime slot1 = threeSlots.get(1);
        final ZonedDateTime slot2 = threeSlots.get(2);
        when(conflictDetectionService.findConflictingAppointments(eq(THERAPIST_ID), eq(slot0), eq(DURATION)))
                .thenReturn(Collections.emptyList());
        when(conflictDetectionService.findConflictingAppointments(eq(THERAPIST_ID), eq(slot1), eq(DURATION)))
                .thenReturn(List.of(conflicting));
        when(conflictDetectionService.findConflictingAppointments(eq(THERAPIST_ID), eq(slot2), eq(DURATION)))
                .thenReturn(Collections.emptyList());

        final Map<Integer, ConflictDetail> result =
                batchConflictDetectionService.detectBatch(THERAPIST_ID, threeSlots, DURATION);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(1);
        assertThat(result.get(1).appointmentId()).isEqualTo(conflicting.getId());
    }

    @Test
    @DisplayName("All slots conflict: returns map with all indices")
    void allSlotsConflictReturnsFullMap() {
        final Appointment conflicting = buildAppointment();
        when(conflictDetectionService.findConflictingAppointments(any(), any(), anyInt()))
                .thenReturn(List.of(conflicting));

        final Map<Integer, ConflictDetail> result =
                batchConflictDetectionService.detectBatch(THERAPIST_ID, threeSlots, DURATION);

        assertThat(result).hasSize(3);
        assertThat(result).containsKeys(0, 1, 2);
    }

    @Test
    @DisplayName("Each slot is checked exactly once")
    void eachSlotCheckedOnce() {
        when(conflictDetectionService.findConflictingAppointments(any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        batchConflictDetectionService.detectBatch(THERAPIST_ID, threeSlots, DURATION);

        verify(conflictDetectionService, times(3))
                .findConflictingAppointments(eq(THERAPIST_ID), any(ZonedDateTime.class), eq(DURATION));
    }

    @Test
    @DisplayName("Empty slot list: returns empty map and makes no service calls")
    void emptySlotListReturnsEmptyMapAndMakesNoCalls() {
        final Map<Integer, ConflictDetail> result =
                batchConflictDetectionService.detectBatch(THERAPIST_ID, Collections.emptyList(), DURATION);

        assertThat(result).isEmpty();
        verify(conflictDetectionService, times(0))
                .findConflictingAppointments(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Conflict detail contains first conflicting appointment ID and start time")
    void conflictDetailContainsFirstAppointmentData() {
        final ZonedDateTime conflictStart = ZonedDateTime.of(2026, 4, 1, 10, 0, 0, 0, NYC);
        final Appointment conflicting = buildAppointmentWithStartTime(conflictStart);

        when(conflictDetectionService.findConflictingAppointments(any(), any(), anyInt()))
                .thenReturn(List.of(conflicting));

        final Map<Integer, ConflictDetail> result =
                batchConflictDetectionService.detectBatch(THERAPIST_ID, threeSlots, DURATION);

        final ConflictDetail detail = result.get(0);
        assertThat(detail).isNotNull();
        assertThat(detail.appointmentId()).isEqualTo(conflicting.getId());
        assertThat(detail.startTime()).isEqualTo(conflictStart);
    }

    @Test
    @DisplayName("When multiple appointments conflict on a slot, only the first is recorded")
    void onlyFirstConflictingAppointmentIsRecorded() {
        final Appointment first = buildAppointment();
        final Appointment second = buildAppointment();

        when(conflictDetectionService.findConflictingAppointments(any(), any(), anyInt()))
                .thenReturn(List.of(first, second));

        final Map<Integer, ConflictDetail> result =
                batchConflictDetectionService.detectBatch(THERAPIST_ID, threeSlots, DURATION);

        assertThat(result.get(0).appointmentId()).isEqualTo(first.getId());
    }

    // ========== Helpers ==========

    private Appointment buildAppointment() {
        return buildAppointmentWithStartTime(ZonedDateTime.of(2026, 4, 1, 10, 0, 0, 0, NYC));
    }

    private Appointment buildAppointmentWithStartTime(final ZonedDateTime startTime) {
        final Appointment appt = new Appointment(
                THERAPIST_ID,
                UUID.randomUUID(),
                null, // session type not needed for conflict detail
                startTime,
                DURATION,
                "America/New_York"
        );
        return appt;
    }
}
