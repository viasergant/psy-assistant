package com.psyassistant.scheduling.dto;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.therapists.domain.TherapistProfile;
import org.springframework.stereotype.Component;

/**
 * Maps appointment entities to calendar-specific DTOs.
 */
@Component
public class CalendarMapper {

    /**
     * Maps an appointment to a lightweight calendar block.
     *
     * @param appointment appointment entity
     * @param therapist therapist profile (for name display)
     * @param client client entity (for name display)
     * @return calendar block DTO
     */
    public CalendarAppointmentBlock toCalendarBlock(final Appointment appointment,
                                                      final TherapistProfile therapist,
                                                      final Client client) {
        return new CalendarAppointmentBlock(
                appointment.getId(),
                appointment.getTherapistProfileId(),
                therapist != null ? therapist.getFullName() : "Unknown",
                appointment.getClientId(),
                client != null ? client.getFullName() : "Unknown",
                appointment.getSessionType().getCode(),
                appointment.getSessionType().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getDurationMinutes(),
                appointment.getStatus(),
                appointment.isModified(),
                appointment.getNotes()
        );
    }
}
