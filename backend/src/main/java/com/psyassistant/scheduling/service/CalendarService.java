package com.psyassistant.scheduling.service;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.dto.CalendarAppointmentBlock;
import com.psyassistant.scheduling.dto.CalendarMapper;
import com.psyassistant.scheduling.dto.CalendarWeekViewResponse;
import com.psyassistant.scheduling.dto.CalendarWeekViewResponse.TherapistInfo;
import com.psyassistant.scheduling.repository.AppointmentRepository;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calendar view operations.
 *
 * <p>Provides day, week, and month views with therapist filtering and multi-therapist support.
 */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarService.class);

    private final AppointmentRepository appointmentRepository;
    private final TherapistProfileRepository therapistProfileRepository;
    private final ClientRepository clientRepository;
    private final CalendarMapper calendarMapper;

    public CalendarService(final AppointmentRepository appointmentRepository,
                            final TherapistProfileRepository therapistProfileRepository,
                            final ClientRepository clientRepository,
                            final CalendarMapper calendarMapper) {
        this.appointmentRepository = appointmentRepository;
        this.therapistProfileRepository = therapistProfileRepository;
        this.clientRepository = clientRepository;
        this.calendarMapper = calendarMapper;
    }

    /**
     * Fetches calendar data for a week view.
     *
     * <p>Week starts on Monday and ends on Sunday (ISO 8601 standard).
     * If no date is provided, uses the current week.
     *
     * @param weekDate any date within the target week (defaults to today)
     * @param therapistIds optional list of therapist IDs to filter (null = all therapists)
     * @param timezone timezone for date calculations (defaults to UTC)
     * @return week view with appointments and therapist metadata
     */
    public CalendarWeekViewResponse getWeekView(final LocalDate weekDate,
                                                  final List<UUID> therapistIds,
                                                  final String timezone) {
        final LocalDate targetDate = weekDate != null ? weekDate : LocalDate.now();
        final ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.of("UTC");

        // Calculate week boundaries (Monday to Sunday)
        final LocalDate weekStart = targetDate.with(DayOfWeek.MONDAY);
        final LocalDate weekEnd = weekStart.plusDays(7);

        LOG.debug("Fetching week view: start={}, end={}, therapistIds={}, timezone={}",
                weekStart, weekEnd, therapistIds, zoneId);

        // Convert to ZonedDateTime for repository query
        final ZonedDateTime startDateTime = weekStart.atStartOfDay(zoneId);
        final ZonedDateTime endDateTime = weekEnd.atStartOfDay(zoneId);

        // Fetch appointments
        final List<Appointment> appointments = appointmentRepository.findByDateRangeAndTherapists(
                startDateTime,
                endDateTime,
                therapistIds
        );

        LOG.debug("Found {} appointments in date range", appointments.size());

        // Extract unique therapist and client IDs
        final List<UUID> uniqueTherapistIds = appointments.stream()
                .map(Appointment::getTherapistProfileId)
                .distinct()
                .toList();

        final List<UUID> uniqueClientIds = appointments.stream()
                .map(Appointment::getClientId)
                .distinct()
                .toList();

        // Batch fetch therapists and clients to avoid N+1 queries
        final Map<UUID, TherapistProfile> therapistMap = therapistProfileRepository.findAllById(uniqueTherapistIds)
                .stream()
                .collect(Collectors.toMap(TherapistProfile::getId, t -> t));

        final Map<UUID, Client> clientMap = clientRepository.findAllById(uniqueClientIds)
                .stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

        // Build therapist info map for UI display
        final Map<UUID, TherapistInfo> therapistInfoMap = new HashMap<>();
        for (final TherapistProfile therapist : therapistMap.values()) {
            therapistInfoMap.put(
                    therapist.getId(),
                    new TherapistInfo(
                            therapist.getId(),
                            therapist.getFullName(),
                            therapist.getSpecialization()
                    )
            );
        }

        // Map appointments to calendar blocks
        final List<CalendarAppointmentBlock> blocks = appointments.stream()
                .map(appointment -> calendarMapper.toCalendarBlock(
                        appointment,
                        therapistMap.get(appointment.getTherapistProfileId()),
                        clientMap.get(appointment.getClientId())
                ))
                .toList();

        return new CalendarWeekViewResponse(weekStart, weekEnd, therapistInfoMap, blocks);
    }
}
