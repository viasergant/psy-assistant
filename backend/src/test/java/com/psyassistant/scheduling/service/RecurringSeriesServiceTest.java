package com.psyassistant.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentSeries;
import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.CancellationType;
import com.psyassistant.scheduling.domain.ConflictResolution;
import com.psyassistant.scheduling.domain.EditScope;
import com.psyassistant.scheduling.domain.RecurrenceType;
import com.psyassistant.scheduling.domain.SeriesStatus;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.dto.CancelRecurringOccurrenceRequest;
import com.psyassistant.scheduling.dto.CreateRecurringSeriesRequest;
import com.psyassistant.scheduling.dto.CreateRecurringSeriesResponse;
import com.psyassistant.scheduling.dto.EditRecurringOccurrenceRequest;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse.ConflictDetail;
import com.psyassistant.scheduling.event.RecurringSlotSkippedEvent;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.AppointmentSeriesRepository;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for {@link RecurringSeriesService}.
 *
 * <p>Covers the core business logic for:
 * <ul>
 *     <li>Conflict pre-flight check (checkConflicts)</li>
 *     <li>Series creation — no conflicts, SKIP_CONFLICTS, ABORT</li>
 *     <li>Single-occurrence edit and isModified flag</li>
 *     <li>Future-series edit applying delta shift</li>
 *     <li>Bulk cancellation — FUTURE_SERIES and ENTIRE_SERIES scope</li>
 *     <li>Single-occurrence cancellation delegation to AppointmentService</li>
 *     <li>EntityNotFoundException for missing series / appointments</li>
 *     <li>IllegalArgumentException when appointment does not belong to series</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringSeriesService Tests")
class RecurringSeriesServiceTest {

    @Mock
    private RecurrencePatternGenerator patternGenerator;

    @Mock
    private BatchConflictDetectionService batchConflictDetection;

    @Mock
    private AppointmentSeriesRepository seriesRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SessionTypeRepository sessionTypeRepository;

    @Mock
    private AppointmentAuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private RecurringSeriesService recurringSeriesService;

    // ========== Test Fixtures ==========

    private static final ZoneId NYC = ZoneId.of("America/New_York");
    private static final UUID THERAPIST_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID SESSION_TYPE_ID = UUID.randomUUID();
    private static final UUID STAFF_ID = UUID.randomUUID();
    private static final String STAFF_NAME = "Dr. Test";
    private static final ZonedDateTime ANCHOR = ZonedDateTime.of(2026, 4, 1, 10, 0, 0, 0, NYC);
    private static final int DURATION = 60;

    private SessionType sessionType;
    private AppointmentSeries savedSeries;

    @BeforeEach
    void setUp() {
        sessionType = new SessionType("IN_PERSON", "In-Person Session", null);

        savedSeries = new AppointmentSeries(
                RecurrenceType.WEEKLY,
                ANCHOR.toLocalDate(),
                4,
                THERAPIST_ID,
                CLIENT_ID,
                sessionType,
                DURATION,
                "America/New_York",
                STAFF_ID
        );
    }

    // ========== checkConflicts ==========

    @Nested
    @DisplayName("checkConflicts")
    class CheckConflicts {

        @Test
        @DisplayName("Returns response with all clean slots when no conflicts")
        void noConflictsAllSlotsClear() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 2)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(THERAPIST_ID, slots, DURATION))
                    .thenReturn(Collections.emptyMap());

            final RecurringConflictCheckResponse resp = recurringSeriesService.checkConflicts(
                    THERAPIST_ID, ANCHOR, DURATION, "America/New_York", RecurrenceType.WEEKLY, 2);

            assertThat(resp.conflictCount()).isEqualTo(0);
            assertThat(resp.cleanSlotCount()).isEqualTo(2);
            assertThat(resp.generatedSlots()).hasSize(2);
            assertThat(resp.generatedSlots().get(0).hasConflict()).isFalse();
        }

        @Test
        @DisplayName("Returns correct conflict count when some slots conflict")
        void someConflictsReturnedInResponse() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1), ANCHOR.plusWeeks(2));
            final ConflictDetail detail = new ConflictDetail(UUID.randomUUID(), null, ANCHOR);
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 3)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(THERAPIST_ID, slots, DURATION))
                    .thenReturn(Map.of(1, detail));

            final RecurringConflictCheckResponse resp = recurringSeriesService.checkConflicts(
                    THERAPIST_ID, ANCHOR, DURATION, "America/New_York", RecurrenceType.WEEKLY, 3);

            assertThat(resp.conflictCount()).isEqualTo(1);
            assertThat(resp.cleanSlotCount()).isEqualTo(2);
            assertThat(resp.generatedSlots().get(1).hasConflict()).isTrue();
            assertThat(resp.generatedSlots().get(0).hasConflict()).isFalse();
        }
    }

    // ========== createSeries ==========

    @Nested
    @DisplayName("createSeries")
    class CreateSeries {

        @Test
        @DisplayName("No conflicts: saves all appointments and returns correct counts")
        void noConflictsSavesAllAppointments() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1), ANCHOR.plusWeeks(2));
            final CreateRecurringSeriesRequest req = buildRequest(3, ConflictResolution.ABORT);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 3)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(eq(THERAPIST_ID), eq(slots), eq(DURATION)))
                    .thenReturn(Collections.emptyMap());
            when(seriesRepository.save(any(AppointmentSeries.class))).thenReturn(savedSeries);
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            final CreateRecurringSeriesResponse resp =
                    recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME);

            assertThat(resp.savedOccurrences()).isEqualTo(3);
            assertThat(resp.skippedOccurrences()).isEqualTo(0);
            verify(appointmentRepository, times(3)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("SKIP_CONFLICTS: conflicting slots are skipped, clean ones are saved")
        void skipConflictsSavesCleanSlots() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1), ANCHOR.plusWeeks(2));
            final ConflictDetail detail = new ConflictDetail(UUID.randomUUID(), null, ANCHOR);
            final CreateRecurringSeriesRequest req = buildRequest(3, ConflictResolution.SKIP_CONFLICTS);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 3)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(eq(THERAPIST_ID), eq(slots), eq(DURATION)))
                    .thenReturn(Map.of(0, detail)); // index 0 conflicts
            when(seriesRepository.save(any(AppointmentSeries.class))).thenReturn(savedSeries);
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            final CreateRecurringSeriesResponse resp =
                    recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME);

            assertThat(resp.savedOccurrences()).isEqualTo(2);
            assertThat(resp.skippedOccurrences()).isEqualTo(1);
            verify(appointmentRepository, times(2)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("SKIP_CONFLICTS: publishes RecurringSlotSkippedEvent for each skipped slot")
        void skipConflictsPublishesEventForSkippedSlot() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1));
            final ConflictDetail detail = new ConflictDetail(UUID.randomUUID(), null, ANCHOR);
            final CreateRecurringSeriesRequest req = buildRequest(2, ConflictResolution.SKIP_CONFLICTS);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 2)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(eq(THERAPIST_ID), eq(slots), eq(DURATION)))
                    .thenReturn(Map.of(1, detail));
            when(seriesRepository.save(any(AppointmentSeries.class))).thenReturn(savedSeries);
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME);

            final ArgumentCaptor<RecurringSlotSkippedEvent> captor =
                    ArgumentCaptor.forClass(RecurringSlotSkippedEvent.class);
            verify(eventPublisher, times(1)).publishEvent(captor.capture());
            assertThat(captor.getValue().recurrenceIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("ABORT: throws SeriesConflictException when conflict exists")
        void abortThrowsSeriesConflictException() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1));
            final ConflictDetail detail = new ConflictDetail(UUID.randomUUID(), null, ANCHOR);
            final CreateRecurringSeriesRequest req = buildRequest(2, ConflictResolution.ABORT);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 2)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(eq(THERAPIST_ID), eq(slots), eq(DURATION)))
                    .thenReturn(Map.of(0, detail));

            assertThatThrownBy(() -> recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME))
                    .isInstanceOf(RecurringSeriesService.SeriesConflictException.class);

            // No appointments or series should be saved
            verify(seriesRepository, never()).save(any());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("No conflicts with ABORT: does not throw, saves normally")
        void abortWithNoConflictsDoesNotThrow() {
            final List<ZonedDateTime> slots = List.of(ANCHOR);
            final CreateRecurringSeriesRequest req = buildRequest(1, ConflictResolution.ABORT);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 1)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(eq(THERAPIST_ID), eq(slots), eq(DURATION)))
                    .thenReturn(Collections.emptyMap());
            when(seriesRepository.save(any(AppointmentSeries.class))).thenReturn(savedSeries);
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            final CreateRecurringSeriesResponse resp =
                    recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME);
            assertThat(resp.savedOccurrences()).isEqualTo(1);
        }

        @Test
        @DisplayName("Unknown session type: throws EntityNotFoundException before any save")
        void unknownSessionTypeThrowsEntityNotFound() {
            final CreateRecurringSeriesRequest req = buildRequest(3, ConflictResolution.ABORT);
            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(seriesRepository, never()).save(any());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Appointments are linked to series and recurrence index is set")
        void appointmentsLinkedToSeriesWithCorrectIndex() {
            final List<ZonedDateTime> slots = List.of(ANCHOR, ANCHOR.plusWeeks(1));
            final CreateRecurringSeriesRequest req = buildRequest(2, ConflictResolution.ABORT);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 2)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(any(), any(), anyInt()))
                    .thenReturn(Collections.emptyMap());
            when(seriesRepository.save(any(AppointmentSeries.class))).thenReturn(savedSeries);

            final ArgumentCaptor<Appointment> apptCaptor = ArgumentCaptor.forClass(Appointment.class);
            when(appointmentRepository.save(apptCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME);

            final List<Appointment> captured = apptCaptor.getAllValues();
            assertThat(captured).hasSize(2);
            assertThat(captured.get(0).getRecurrenceIndex()).isEqualTo(0);
            assertThat(captured.get(1).getRecurrenceIndex()).isEqualTo(1);
            assertThat(captured.get(0).getSeries()).isSameAs(savedSeries);
        }

        @Test
        @DisplayName("Audit is recorded after successful creation")
        void auditRecordedAfterCreation() {
            final List<ZonedDateTime> slots = List.of(ANCHOR);
            final CreateRecurringSeriesRequest req = buildRequest(1, ConflictResolution.ABORT);

            when(sessionTypeRepository.findById(SESSION_TYPE_ID)).thenReturn(Optional.of(sessionType));
            when(patternGenerator.generate(ANCHOR, RecurrenceType.WEEKLY, 1)).thenReturn(slots);
            when(batchConflictDetection.detectBatch(any(), any(), anyInt()))
                    .thenReturn(Collections.emptyMap());
            when(seriesRepository.save(any(AppointmentSeries.class))).thenReturn(savedSeries);
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            recurringSeriesService.createSeries(req, STAFF_ID, STAFF_NAME);

            verify(auditService).recordSeriesCreated(
                    eq(savedSeries), anyList(), eq(STAFF_ID), eq(STAFF_NAME));
        }
    }

    // ========== editOccurrence — SINGLE scope ==========

    @Nested
    @DisplayName("editOccurrence — SINGLE scope")
    class EditSingleOccurrence {

        @Test
        @DisplayName("Sets isModified=true on the appointment")
        void singleEditSetsIsModified() {
            final Appointment appt = buildAppointment(savedSeries, 0);
            final UUID apptId = appt.getId();

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.SINGLE, ANCHOR.plusHours(1), null, null, null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            final Appointment result = recurringSeriesService.editOccurrence(
                    savedSeries.getId() != null ? savedSeries.getId() : 1L,
                    apptId, req, STAFF_ID, STAFF_NAME);

            assertThat(result.isModified()).isTrue();
        }

        @Test
        @DisplayName("Updates start time when provided")
        void singleEditUpdatesStartTime() {
            final Appointment appt = buildAppointment(savedSeries, 0);
            final UUID apptId = appt.getId();
            final ZonedDateTime newStart = ANCHOR.plusHours(2);

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.SINGLE, newStart, null, null, null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            final Appointment result = recurringSeriesService.editOccurrence(
                    getSeriesId(), apptId, req, STAFF_ID, STAFF_NAME);

            assertThat(result.getStartTime()).isEqualTo(newStart);
        }

        @Test
        @DisplayName("Updates notes when provided")
        void singleEditUpdatesNotes() {
            final Appointment appt = buildAppointment(savedSeries, 0);
            final UUID apptId = appt.getId();

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.SINGLE, null, null, "Special requirements note", null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            final Appointment result = recurringSeriesService.editOccurrence(
                    getSeriesId(), apptId, req, STAFF_ID, STAFF_NAME);

            assertThat(result.getNotes()).isEqualTo("Special requirements note");
        }

        @Test
        @DisplayName("Records audit for single occurrence edit")
        void singleEditRecordsAudit() {
            final Appointment appt = buildAppointment(savedSeries, 0);
            final UUID apptId = appt.getId();

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.SINGLE, ANCHOR.plusHours(1), null, null, null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

            recurringSeriesService.editOccurrence(getSeriesId(), apptId, req, STAFF_ID, STAFF_NAME);

            verify(auditService).recordOccurrenceEdit(any(Appointment.class), eq(EditScope.SINGLE),
                    eq(STAFF_ID), eq(STAFF_NAME));
        }

        @Test
        @DisplayName("Appointment not in requested series throws IllegalArgumentException")
        void appointmentNotInSeriesThrowsIllegalArgument() {
            final AppointmentSeries otherSeries = buildSeriesWithId(99L);
            final Appointment appt = buildAppointment(otherSeries, 0);
            final UUID apptId = appt.getId();

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.SINGLE, ANCHOR.plusHours(1), null, null, null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));

            assertThatThrownBy(() ->
                    recurringSeriesService.editOccurrence(42L, apptId, req, STAFF_ID, STAFF_NAME))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ========== editOccurrence — FUTURE_SERIES scope ==========

    @Nested
    @DisplayName("editOccurrence — FUTURE_SERIES scope")
    class EditFutureSeries {

        @Test
        @DisplayName("Updates all future appointments by time delta")
        void futureEditAppliesTimeDelta() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 2, ANCHOR);
            final UUID apptId = anchor.getId();
            final ZonedDateTime newStart = ANCHOR.plusHours(1); // +60 min delta

            final Appointment future3 = buildAppointmentWithIndex(savedSeries, 2, ANCHOR);
            final Appointment future4 = buildAppointmentWithIndex(savedSeries, 3, ANCHOR.plusWeeks(1));

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.FUTURE_SERIES, newStart, null, null, null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.findBySeriesIdFromIndex(seriesId, 2, AppointmentStatus.CANCELLED))
                    .thenReturn(List.of(future3, future4));
            when(appointmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));

            recurringSeriesService.editOccurrence(seriesId, apptId, req, STAFF_ID, STAFF_NAME);

            // Both future appointments should have shifted by +60 minutes
            assertThat(future3.getStartTime()).isEqualTo(ANCHOR.plusHours(1));
            assertThat(future4.getStartTime()).isEqualTo(ANCHOR.plusWeeks(1).plusHours(1));
        }

        @Test
        @DisplayName("Records audit for future series edit")
        void futureEditRecordsAudit() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 0, ANCHOR);
            final UUID apptId = anchor.getId();
            final ZonedDateTime newStart = ANCHOR.plusHours(1);

            final EditRecurringOccurrenceRequest req = new EditRecurringOccurrenceRequest(
                    EditScope.FUTURE_SERIES, newStart, null, null, null);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.findBySeriesIdFromIndex(seriesId, 0, AppointmentStatus.CANCELLED))
                    .thenReturn(List.of(anchor));
            when(appointmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));

            recurringSeriesService.editOccurrence(seriesId, apptId, req, STAFF_ID, STAFF_NAME);

            verify(auditService).recordFutureSeriesEdit(
                    eq(savedSeries), eq(apptId), eq(1), eq(STAFF_ID), eq(STAFF_NAME));
        }
    }

    // ========== cancelOccurrences ==========

    @Nested
    @DisplayName("cancelOccurrences")
    class CancelOccurrences {

        @Test
        @DisplayName("SINGLE scope: delegates to AppointmentService.cancelAppointment")
        void singleScopeDelegatesToAppointmentService() {
            final UUID apptId = UUID.randomUUID();
            final CancelRecurringOccurrenceRequest req = new CancelRecurringOccurrenceRequest(
                    EditScope.SINGLE, "Client requested cancellation today", CancellationType.CLIENT_INITIATED);

            recurringSeriesService.cancelOccurrences(
                    getSeriesId(), apptId, req, STAFF_ID, STAFF_NAME, appointmentService);

            verify(appointmentService).cancelAppointment(eq(apptId), eq(CancellationType.CLIENT_INITIATED),
                    eq("Client requested cancellation today"), eq(STAFF_ID), eq(STAFF_NAME));
            verify(appointmentRepository, never()).bulkCancelFromIndex(anyLong(), anyInt(), anyString(), anyString());
        }

        @Test
        @DisplayName("FUTURE_SERIES scope: calls bulkCancelFromIndex from anchor's recurrenceIndex")
        void futureSeriesCallsBulkCancelFromAnchorIndex() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 3, ANCHOR);
            final UUID apptId = anchor.getId();

            final CancelRecurringOccurrenceRequest req = new CancelRecurringOccurrenceRequest(
                    EditScope.FUTURE_SERIES,
                    "Therapist illness — cancelling future sessions",
                    CancellationType.THERAPIST_INITIATED);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.bulkCancelFromIndex(eq(seriesId), eq(3), anyString(), anyString()))
                    .thenReturn(5);
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));
            when(appointmentRepository.findBySeriesIdFromIndex(eq(seriesId), eq(0), eq(AppointmentStatus.CANCELLED)))
                    .thenReturn(Collections.emptyList());

            recurringSeriesService.cancelOccurrences(seriesId, apptId, req, STAFF_ID, STAFF_NAME, appointmentService);

            verify(appointmentRepository).bulkCancelFromIndex(
                    eq(seriesId), eq(3), anyString(), eq("THERAPIST_INITIATED"));
        }

        @Test
        @DisplayName("ENTIRE_SERIES scope: calls bulkCancelFromIndex with fromIndex=0")
        void entireSeriesCallsBulkCancelFromZero() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 5, ANCHOR);
            final UUID apptId = anchor.getId();

            final CancelRecurringOccurrenceRequest req = new CancelRecurringOccurrenceRequest(
                    EditScope.ENTIRE_SERIES,
                    "Series cancelled entirely by client request",
                    CancellationType.CLIENT_INITIATED);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.bulkCancelFromIndex(eq(seriesId), eq(0), anyString(), anyString()))
                    .thenReturn(8);
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));
            when(appointmentRepository.findBySeriesIdFromIndex(eq(seriesId), eq(0), eq(AppointmentStatus.CANCELLED)))
                    .thenReturn(Collections.emptyList());

            recurringSeriesService.cancelOccurrences(seriesId, apptId, req, STAFF_ID, STAFF_NAME, appointmentService);

            verify(appointmentRepository).bulkCancelFromIndex(eq(seriesId), eq(0), anyString(), eq("CLIENT_INITIATED"));
        }

        @Test
        @DisplayName("Bulk cancel: updates series status to CANCELLED when no remaining appointments")
        void bulkCancelSetsSeriesStatusCancelled() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 0, ANCHOR);
            final UUID apptId = anchor.getId();

            final CancelRecurringOccurrenceRequest req = new CancelRecurringOccurrenceRequest(
                    EditScope.ENTIRE_SERIES,
                    "Full series cancellation due to therapist departure",
                    CancellationType.THERAPIST_INITIATED);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.bulkCancelFromIndex(anyLong(), anyInt(), anyString(), anyString()))
                    .thenReturn(4);
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));
            // No remaining (all returned appointments are CANCELLED — excluded by query)
            when(appointmentRepository.findBySeriesIdFromIndex(eq(seriesId), eq(0), eq(AppointmentStatus.CANCELLED)))
                    .thenReturn(Collections.emptyList());

            recurringSeriesService.cancelOccurrences(seriesId, apptId, req, STAFF_ID, STAFF_NAME, appointmentService);

            assertThat(savedSeries.getStatus()).isEqualTo(SeriesStatus.CANCELLED);
            verify(seriesRepository, atLeastOnce()).save(savedSeries);
        }

        @Test
        @DisplayName("Bulk cancel: updates series status to PARTIALLY_CANCELLED when some remain")
        void bulkCancelSetsSeriesStatusPartiallyCancelled() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 2, ANCHOR);
            final UUID apptId = anchor.getId();

            // Build a remaining (non-cancelled) appointment to simulate "some remain"
            final Appointment remaining = buildAppointmentWithIndex(savedSeries, 0, ANCHOR.minusWeeks(2));

            final CancelRecurringOccurrenceRequest req = new CancelRecurringOccurrenceRequest(
                    EditScope.FUTURE_SERIES,
                    "Therapist on leave from this date forward",
                    CancellationType.THERAPIST_INITIATED);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.bulkCancelFromIndex(anyLong(), anyInt(), anyString(), anyString()))
                    .thenReturn(2);
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));
            // One SCHEDULED remains at index 0
            when(appointmentRepository.findBySeriesIdFromIndex(eq(seriesId), eq(0), eq(AppointmentStatus.CANCELLED)))
                    .thenReturn(List.of(remaining));

            recurringSeriesService.cancelOccurrences(seriesId, apptId, req, STAFF_ID, STAFF_NAME, appointmentService);

            assertThat(savedSeries.getStatus()).isEqualTo(SeriesStatus.PARTIALLY_CANCELLED);
        }

        @Test
        @DisplayName("Bulk cancel: records audit with correct scope and series ID")
        void bulkCancelRecordsAudit() {
            final long seriesId = getSeriesId();
            final Appointment anchor = buildAppointmentWithIndex(savedSeries, 0, ANCHOR);
            final UUID apptId = anchor.getId();

            final CancelRecurringOccurrenceRequest req = new CancelRecurringOccurrenceRequest(
                    EditScope.ENTIRE_SERIES,
                    "Practice closing — all sessions cancelled",
                    CancellationType.THERAPIST_INITIATED);

            when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(anchor));
            when(appointmentRepository.bulkCancelFromIndex(anyLong(), anyInt(), anyString(), anyString()))
                    .thenReturn(3);
            when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(savedSeries));
            when(appointmentRepository.findBySeriesIdFromIndex(any(), anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            recurringSeriesService.cancelOccurrences(seriesId, apptId, req, STAFF_ID, STAFF_NAME, appointmentService);

            verify(auditService).recordSeriesCancellation(
                    eq(seriesId), eq(0), eq(3), eq(EditScope.ENTIRE_SERIES), eq(STAFF_ID), eq(STAFF_NAME));
        }
    }

    // ========== getSeries ==========

    @Nested
    @DisplayName("getSeries")
    class GetSeries {

        @Test
        @DisplayName("Returns series when found")
        void returnsSeriesWhenFound() {
            when(seriesRepository.findById(1L)).thenReturn(Optional.of(savedSeries));

            final AppointmentSeries result = recurringSeriesService.getSeries(1L);

            assertThat(result).isSameAs(savedSeries);
        }

        @Test
        @DisplayName("Throws EntityNotFoundException when series does not exist")
        void throwsWhenSeriesNotFound() {
            when(seriesRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recurringSeriesService.getSeries(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ========== SeriesConflictException ==========

    @Nested
    @DisplayName("SeriesConflictException")
    class SeriesConflictExceptionTests {

        @Test
        @DisplayName("Exposes conflictMap via getConflictMap()")
        void exceptionExposesConflictMap() {
            final ConflictDetail detail = new ConflictDetail(UUID.randomUUID(), null, ANCHOR);
            final Map<Integer, ConflictDetail> map = Map.of(0, detail);

            final RecurringSeriesService.SeriesConflictException ex =
                    new RecurringSeriesService.SeriesConflictException("test conflict", map);

            assertThat(ex.getConflictMap()).isSameAs(map);
            assertThat(ex.getMessage()).contains("test conflict");
        }
    }

    // ========== Helper Methods ==========

    private CreateRecurringSeriesRequest buildRequest(final int occurrences,
                                                       final ConflictResolution resolution) {
        return new CreateRecurringSeriesRequest(
                THERAPIST_ID,
                CLIENT_ID,
                SESSION_TYPE_ID,
                ANCHOR,
                DURATION,
                "America/New_York",
                RecurrenceType.WEEKLY,
                occurrences,
                null,
                resolution
        );
    }

    private long getSeriesId() {
        // savedSeries has no DB-assigned ID (it's BIGSERIAL) so we use reflection to inject one,
        // or rely on a mock that returns a fixed value.
        // Since @GeneratedValue fields are set by JPA, we return a stub value.
        return 1L;
    }

    private AppointmentSeries buildSeriesWithId(final long id) {
        final AppointmentSeries series = new AppointmentSeries(
                RecurrenceType.WEEKLY,
                ANCHOR.toLocalDate(),
                4,
                THERAPIST_ID,
                CLIENT_ID,
                sessionType,
                DURATION,
                "America/New_York",
                STAFF_ID
        );
        org.springframework.test.util.ReflectionTestUtils.setField(series, "id", id);
        return series;
    }

    private Appointment buildAppointment(final AppointmentSeries series, final int index) {
        return buildAppointmentWithIndex(series, index, ANCHOR.plusWeeks(index));
    }

    private Appointment buildAppointmentWithIndex(final AppointmentSeries series,
                                                   final int index,
                                                   final ZonedDateTime startTime) {
        final AppointmentSeries resolvedSeries = (series.getId() != null)
                ? series
                : buildSeriesWithId(1L);
        final Appointment appt = new Appointment(
                THERAPIST_ID,
                CLIENT_ID,
                sessionType,
                startTime,
                DURATION,
                "America/New_York"
        );
        appt.setSeries(resolvedSeries);
        appt.setRecurrenceIndex(index);
        return appt;
    }
}
