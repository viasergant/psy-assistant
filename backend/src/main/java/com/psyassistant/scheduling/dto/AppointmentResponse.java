package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.AppointmentStatus;
import com.psyassistant.scheduling.domain.CancellationType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response DTO for appointment details.
 *
 * <p>Includes full appointment information for display and editing.
 */
public record AppointmentResponse(
        UUID id,
        UUID therapistProfileId,
        UUID clientId,
        SessionTypeInfo sessionType,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        Integer durationMinutes,
        String timezone,
        AppointmentStatus status,
        Boolean isConflictOverride,
        CancellationType cancellationType,
        String cancellationReason,
        Instant cancelledAt,
        UUID cancelledBy,
        String rescheduleReason,
        ZonedDateTime originalStartTime,
        Instant rescheduledAt,
        UUID rescheduledBy,
        String notes,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
    /**
     * Nested session type information.
     */
    public record SessionTypeInfo(
            UUID id,
            String code,
            String name,
            String description
    ) {
    }
}
