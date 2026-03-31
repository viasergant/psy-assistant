package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.TherapistLeave;
import com.psyassistant.scheduling.domain.TherapistRecurringSchedule;
import com.psyassistant.scheduling.domain.TherapistScheduleOverride;
import com.psyassistant.scheduling.repository.TherapistLeaveRepository;
import com.psyassistant.scheduling.repository.TherapistRecurringScheduleRepository;
import com.psyassistant.scheduling.repository.TherapistScheduleOverrideRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for computing therapist availability slots.
 *
 * <p>Combines data from:
 * <ul>
 *     <li>Recurring weekly schedule (baseline availability)</li>
 *     <li>Schedule overrides (take precedence over recurring)</li>
 *     <li>Approved leave periods (blocks all dates in range)</li>
 *     <li>Future: existing appointments (not implemented in this phase)</li>
 * </ul>
 *
 * <p>Generates 30-minute slots respecting all constraints.
 */
@Service
public class AvailabilityQueryService {

    private final TherapistRecurringScheduleRepository recurringScheduleRepository;
    private final TherapistScheduleOverrideRepository overrideRepository;
    private final TherapistLeaveRepository leaveRepository;

    /**
     * Constructs the availability query service.
     *
     * @param recurringScheduleRepository repository for recurring schedules
     * @param overrideRepository repository for overrides
     * @param leaveRepository repository for leave periods
     */
    public AvailabilityQueryService(final TherapistRecurringScheduleRepository recurringScheduleRepository,
                                     final TherapistScheduleOverrideRepository overrideRepository,
                                     final TherapistLeaveRepository leaveRepository) {
        this.recurringScheduleRepository = recurringScheduleRepository;
        this.overrideRepository = overrideRepository;
        this.leaveRepository = leaveRepository;
    }

    /**
     * Computes available 30-minute slots for a therapist across a date range.
     *
     * <p>Algorithm:
     * <ol>
     *     <li>For each date in range, check if blocked by approved leave → skip if yes</li>
     *     <li>Check for override on that date → use override hours if present</li>
     *     <li>Otherwise, use recurring schedule for that day of week</li>
     *     <li>Generate 30-minute slots within available hours</li>
     * </ol>
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate query start date (inclusive)
     * @param endDate query end date (inclusive)
     * @return list of available slots, each representing a 30-minute window
     */
    @Transactional(readOnly = true)
    public List<AvailabilitySlot> computeAvailableSlots(final UUID therapistProfileId,
                                                         final LocalDate startDate,
                                                         final LocalDate endDate) {
        // Fetch all relevant data upfront
        final List<TherapistRecurringSchedule> recurringSchedules =
            recurringScheduleRepository.findByTherapistProfileId(therapistProfileId);

        final List<TherapistScheduleOverride> overrides =
            overrideRepository.findByTherapistProfileIdAndDateBetween(therapistProfileId, startDate, endDate);

        final List<TherapistLeave> approvedLeave =
            leaveRepository.findApprovedLeaveOverlapping(therapistProfileId, startDate, endDate);

        // Build lookup maps for fast access
        final Map<Integer, List<TherapistRecurringSchedule>> recurringByDay =
            recurringSchedules.stream()
                .collect(Collectors.groupingBy(TherapistRecurringSchedule::getDayOfWeek));

        final Map<LocalDate, TherapistScheduleOverride> overrideByDate =
            overrides.stream()
                .collect(Collectors.toMap(TherapistScheduleOverride::getOverrideDate, o -> o));

        final Set<LocalDate> leaveDates = buildLeaveDateSet(approvedLeave);

        // Generate slots for each date in range
        final List<AvailabilitySlot> slots = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // Skip if date is blocked by approved leave
            if (leaveDates.contains(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Check for override
            final TherapistScheduleOverride override = overrideByDate.get(currentDate);
            if (override != null) {
                if (override.getIsAvailable()) {
                    // Use override hours
                    slots.addAll(generateSlotsForTimeRange(
                        currentDate,
                        override.getStartTime(),
                        override.getEndTime()
                    ));
                }
                // If override is not available, skip this date (no slots)
            } else {
                // Use recurring schedule for this day of week
                final int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
                final List<TherapistRecurringSchedule> daySchedules = recurringByDay.get(dayOfWeek);

                if (daySchedules != null) {
                    for (final TherapistRecurringSchedule schedule : daySchedules) {
                        slots.addAll(generateSlotsForTimeRange(
                            currentDate,
                            schedule.getStartTime(),
                            schedule.getEndTime()
                        ));
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return slots;
    }

    /**
     * Generates 30-minute slots for a single time range on a specific date.
     *
     * @param date the date
     * @param startTime start time
     * @param endTime end time
     * @return list of 30-minute slots within the time range
     */
    private List<AvailabilitySlot> generateSlotsForTimeRange(final LocalDate date,
                                                               final LocalTime startTime,
                                                               final LocalTime endTime) {
        final List<AvailabilitySlot> slots = new ArrayList<>();
        LocalTime currentTime = startTime;

        while (currentTime.isBefore(endTime)) {
            final LocalTime slotEnd = currentTime.plusMinutes(30);
            if (slotEnd.isAfter(endTime)) {
                break; // Don't create partial slot beyond end time
            }

            slots.add(new AvailabilitySlot(date, currentTime, slotEnd));
            currentTime = slotEnd;
        }

        return slots;
    }

    /**
     * Builds a set of all dates covered by approved leave periods.
     *
     * @param approvedLeave list of approved leave periods
     * @return set of dates blocked by leave
     */
    private Set<LocalDate> buildLeaveDateSet(final List<TherapistLeave> approvedLeave) {
        final Set<LocalDate> leaveDates = new java.util.HashSet<>();

        for (final TherapistLeave leave : approvedLeave) {
            LocalDate current = leave.getStartDate();
            while (!current.isAfter(leave.getEndDate())) {
                leaveDates.add(current);
                current = current.plusDays(1);
            }
        }

        return leaveDates;
    }

    /**
     * Represents a single 30-minute availability slot.
     *
     * @param date the date
     * @param startTime start time of the slot
     * @param endTime end time of the slot (start + 30 minutes)
     */
    public record AvailabilitySlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
    }
}
