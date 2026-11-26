package com.psyassistant.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.TherapistRecurringSchedule;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.scheduling.repository.TherapistLeaveRepository;
import com.psyassistant.scheduling.repository.TherapistRecurringScheduleRepository;
import com.psyassistant.scheduling.repository.TherapistScheduleOverrideRepository;
import com.psyassistant.scheduling.service.AvailabilityQueryService.AvailabilitySlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for booked-appointment handling in {@link AvailabilityQueryService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Availability Query Service Tests")
class AvailabilityQueryServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 30);
    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    @Mock
    private TherapistRecurringScheduleRepository recurringRepository;
    @Mock
    private TherapistScheduleOverrideRepository overrideRepository;
    @Mock
    private TherapistLeaveRepository leaveRepository;
    @Mock
    private AppointmentRepository appointmentRepository;

    private AvailabilityQueryService service;
    private final UUID therapistId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AvailabilityQueryService(
            recurringRepository, overrideRepository, leaveRepository, appointmentRepository);
        final TherapistRecurringSchedule monday = new TherapistRecurringSchedule(
            therapistId, 1, LocalTime.of(10, 0), LocalTime.of(12, 0), KYIV.getId());
        when(recurringRepository.findByTherapistProfileId(therapistId)).thenReturn(List.of(monday));
    }

    private Appointment appointmentAt(final int hour, final int minute, final int durationMinutes) {
        return new Appointment(therapistId, UUID.randomUUID(), null,
            ZonedDateTime.of(MONDAY.getYear(), MONDAY.getMonthValue(), MONDAY.getDayOfMonth(),
                hour, minute, 0, 0, KYIV),
            durationMinutes, KYIV.getId());
    }

    @Test
    @DisplayName("returns all slots when nothing is booked")
    void returnsAllSlotsWhenNothingBooked() {
        when(appointmentRepository.findByTherapistAndDateRange(any(), any(), any())).thenReturn(List.of());

        assertThat(service.computeAvailableSlots(therapistId, MONDAY, MONDAY)).hasSize(4);
    }

    @Test
    @DisplayName("removes only the slots covered by a booked appointment")
    void removesSlotsCoveredByAppointment() {
        when(appointmentRepository.findByTherapistAndDateRange(any(), any(), any()))
            .thenReturn(List.of(appointmentAt(10, 30, 60)));

        final List<AvailabilitySlot> slots = service.computeAvailableSlots(therapistId, MONDAY, MONDAY);

        assertThat(slots).extracting(AvailabilitySlot::startTime)
            .containsExactly(LocalTime.of(10, 0), LocalTime.of(11, 30));
    }

    @Test
    @DisplayName("an appointment ending exactly at a slot start does not block that slot")
    void adjacentAppointmentDoesNotBlock() {
        when(appointmentRepository.findByTherapistAndDateRange(any(), any(), any()))
            .thenReturn(List.of(appointmentAt(9, 0, 60)));

        assertThat(service.computeAvailableSlots(therapistId, MONDAY, MONDAY)).hasSize(4);
    }

    @Test
    @DisplayName("a 15-minute appointment blocks the 30-minute slot it falls in")
    void shortAppointmentBlocksWholeSlot() {
        when(appointmentRepository.findByTherapistAndDateRange(any(), any(), any()))
            .thenReturn(List.of(appointmentAt(11, 15, 15)));

        assertThat(service.computeAvailableSlots(therapistId, MONDAY, MONDAY))
            .extracting(AvailabilitySlot::startTime)
            .doesNotContain(LocalTime.of(11, 0));
    }
}
