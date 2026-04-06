package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.AppointmentStatus;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Lightweight appointment representation for calendar views.
 *
 * <p>Optimized for rendering appointment blocks in day/week/month grids.
 * Contains only the fields needed for visual display and click-through to detail.
 */
public record CalendarAppointmentBlock(
        UUID id,
        UUID therapistProfileId,
        String therapistName,
        UUID clientId,
        String clientName,
        String sessionTypeCode,
        String sessionTypeName,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        Integer durationMinutes,
        AppointmentStatus status,
        boolean isModified,
        String notes
) {
}
