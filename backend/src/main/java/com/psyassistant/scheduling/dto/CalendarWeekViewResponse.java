package com.psyassistant.scheduling.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response for calendar week view.
 *
 * <p>Provides week boundaries and appointments grouped by therapist for efficient rendering.
 * Frontend uses this to build the week grid with therapist columns.
 */
public record CalendarWeekViewResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        Map<UUID, TherapistInfo> therapists,
        List<CalendarAppointmentBlock> appointments
) {
    /**
     * Therapist metadata for column headers.
     */
    public record TherapistInfo(
            UUID id,
            String name,
            String specialization
    ) {
    }
}
