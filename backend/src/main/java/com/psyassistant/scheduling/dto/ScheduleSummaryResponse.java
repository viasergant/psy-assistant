package com.psyassistant.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing a therapist's complete schedule summary.
 *
 * <p>Includes recurring weekly hours, overrides, and leave periods.
 */
public record ScheduleSummaryResponse(
    UUID therapistProfileId,
    List<RecurringScheduleEntry> recurringSchedule,
    List<OverrideEntry> overrides,
    List<LeaveEntry> leavePeriods
) {
    /**
     * Recurring schedule entry.
     */
    public record RecurringScheduleEntry(
        UUID id,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String timezone
    ) {
    }

    /**
     * Override entry.
     */
    public record OverrideEntry(
        UUID id,
        LocalDate overrideDate,
        Boolean isAvailable,
        LocalTime startTime,
        LocalTime endTime,
        String reason
    ) {
    }

    /**
     * Leave entry.
     */
    public record LeaveEntry(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        String leaveType,
        String status,
        String requestNotes,
        String adminNotes
    ) {
    }
}
