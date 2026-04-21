package com.psyassistant.sessions.dto;

import com.psyassistant.sessions.domain.AttendanceOutcome;
import com.psyassistant.sessions.domain.RecordKind;
import com.psyassistant.sessions.domain.SessionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for session record details.
 *
 * <p>Includes full session record information for display and editing.
 * For GROUP sessions, {@code clientId} and {@code clientName} are null;
 * the {@code participants} list carries per-client data instead.
 */
public record SessionRecordResponse(
        UUID id,
        UUID appointmentId,
        RecordKind recordKind,
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
        String createdBy,
        /** Non-empty only for GROUP session records. */
        List<GroupSessionParticipantResponse> participants
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
