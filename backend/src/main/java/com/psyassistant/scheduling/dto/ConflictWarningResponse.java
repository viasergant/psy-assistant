package com.psyassistant.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO providing conflict warnings when submitting a leave request.
 *
 * <p>Contains details of existing appointments that overlap with the requested leave period.
 */
public record ConflictWarningResponse(
    boolean hasConflicts,
    int conflictCount,
    List<AppointmentConflict> conflicts
) {
    public record AppointmentConflict(
        LocalDate date,
        LocalTime time,
        String clientName,
        String appointmentType
    ) {
    }
}
