package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import com.psyassistant.sessions.domain.SessionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for session record details.
 *
 * <p>Includes full session record information for display and editing.
 */
public record SessionRecordResponse(
        UUID id,
        UUID appointmentId,
        UUID clientId,
        String clientName,
        UUID therapistId,
        LocalDate sessionDate,
        LocalTime scheduledStartTime,
        SessionTypeInfo sessionType,
        Duration plannedDuration,
        SessionStatus status,
        String cancellationReason,
        String sessionNotes,
        LocalTime actualEndTime,
        AttendanceOutcome attendanceOutcome,
        Instant cancelledAt,
        UUID cancellationInitiatorId,
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
    ) { }
}
