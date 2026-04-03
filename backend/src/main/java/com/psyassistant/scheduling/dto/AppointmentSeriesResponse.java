package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.RecurrenceType;
import com.psyassistant.scheduling.domain.SeriesStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a recurring appointment series, including all its occurrences.
 */
public record AppointmentSeriesResponse(
        Long seriesId,
        RecurrenceType recurrenceType,
        LocalDate startDate,
        int totalOccurrences,
        int generatedOccurrences,
        UUID therapistProfileId,
        UUID clientId,
        AppointmentResponse.SessionTypeInfo sessionType,
        int durationMinutes,
        String timezone,
        SeriesStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        List<AppointmentResponse> appointments
) {
}
