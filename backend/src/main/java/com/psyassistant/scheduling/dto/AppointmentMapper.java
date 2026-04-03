package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.Appointment;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link Appointment} entities and DTOs.
 *
 * <p>Centralizes DTO mapping logic to avoid duplication across controllers and services.
 */
@Component
public class AppointmentMapper {

    /**
     * Converts an appointment entity to a response DTO.
     *
     * @param appointment appointment entity
     * @return response DTO with full appointment details
     */
    public AppointmentResponse toResponse(final Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        final AppointmentResponse.SessionTypeInfo sessionTypeInfo = new AppointmentResponse.SessionTypeInfo(
                appointment.getSessionType().getId(),
                appointment.getSessionType().getCode(),
                appointment.getSessionType().getName(),
                appointment.getSessionType().getDescription()
        );

        final Long seriesId = appointment.getSeries() != null ? appointment.getSeries().getId() : null;

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getTherapistProfileId(),
                appointment.getClientId(),
                sessionTypeInfo,
                appointment.getStartTime(),
                appointment.getEndTime(), // Calculated field
                appointment.getDurationMinutes(),
                appointment.getTimezone(),
                appointment.getStatus(),
                appointment.getIsConflictOverride(),
                appointment.getCancellationType(),
                appointment.getCancellationReason(),
                appointment.getCancelledAt(),
                appointment.getCancelledBy(),
                appointment.getRescheduleReason(),
                appointment.getOriginalStartTime(),
                appointment.getRescheduledAt(),
                appointment.getRescheduledBy(),
                appointment.getNotes(),
                appointment.getVersion(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getCreatedBy(),
                seriesId,
                appointment.getRecurrenceIndex(),
                appointment.isModified()
        );
    }

    /**
     * Converts a conflict into a minimal conflict DTO for UI display.
     *
     * <p>Note: {@code clientName} is null in this implementation - should be joined
     * from client table in production (requires repository enhancement).
     *
     * @param conflict conflicting appointment
     * @return conflict DTO
     */
    public ConflictCheckResponse.ConflictingAppointment toConflictDto(final Appointment conflict) {
        return new ConflictCheckResponse.ConflictingAppointment(
                conflict.getId(),
                conflict.getClientId(),
                null, // TODO: Join client name from client table
                conflict.getStartTime(),
                conflict.getEndTime(),
                conflict.getDurationMinutes()
        );
    }
}
