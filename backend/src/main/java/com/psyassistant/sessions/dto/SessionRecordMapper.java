package com.psyassistant.sessions.dto;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.sessions.domain.SessionRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link SessionRecord} entities and DTOs.
 *
 * <p>Centralizes DTO mapping logic to avoid duplication across controllers and services.
 */
@Component
public class SessionRecordMapper {

    private final ClientRepository clientRepository;

    /**
     * Constructs a new SessionRecordMapper.
     *
     * @param clientRepository the client repository
     */
    public SessionRecordMapper(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * Converts a session record entity to a response DTO.
     *
     * @param sessionRecord session record entity
     * @return response DTO with full session record details
     */
    public SessionRecordResponse toResponse(final SessionRecord sessionRecord) {
        if (sessionRecord == null) {
            return null;
        }

        // Fetch client name
        final String clientName = clientRepository.findById(sessionRecord.getClientId())
                .map(Client::getFullName)
                .orElse("Unknown Client");

        final SessionRecordResponse.SessionTypeInfo sessionTypeInfo =
                new SessionRecordResponse.SessionTypeInfo(
                        sessionRecord.getSessionType().getId(),
                        sessionRecord.getSessionType().getCode(),
                        sessionRecord.getSessionType().getName(),
                        sessionRecord.getSessionType().getDescription()
                );

        return new SessionRecordResponse(
                sessionRecord.getId(),
                sessionRecord.getAppointmentId(),
                sessionRecord.getClientId(),
                clientName,
                sessionRecord.getTherapistId(),
                sessionRecord.getSessionDate(),
                sessionRecord.getScheduledStartTime(),
                sessionTypeInfo,
                sessionRecord.getPlannedDuration(),
                sessionRecord.getStatus(),
                sessionRecord.getCancellationReason(),
                sessionRecord.getSessionNotes(),
                sessionRecord.getActualEndTime(),
                sessionRecord.getCreatedAt(),
                sessionRecord.getUpdatedAt(),
                sessionRecord.getCreatedBy()
        );
    }
}
