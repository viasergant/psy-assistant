package com.psyassistant.sessions.dto;

import com.psyassistant.crm.clients.Client;
import com.psyassistant.crm.clients.ClientRepository;
import com.psyassistant.sessions.domain.GroupSessionAttendance;
import com.psyassistant.sessions.domain.RecordKind;
import com.psyassistant.sessions.domain.SessionParticipant;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.repository.GroupSessionAttendanceRepository;
import com.psyassistant.sessions.repository.SessionParticipantRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link SessionRecord} entities and DTOs.
 *
 * <p>Centralizes DTO mapping logic to avoid duplication across controllers and services.
 * Handles both INDIVIDUAL and GROUP session record types.
 */
@Component
public class SessionRecordMapper {

    private final ClientRepository clientRepository;
    private final SessionParticipantRepository participantRepository;
    private final GroupSessionAttendanceRepository attendanceRepository;

    /**
     * Constructs a new SessionRecordMapper.
     *
     * @param clientRepository      the client repository
     * @param participantRepository the session participant repository
     * @param attendanceRepository  the group session attendance repository
     */
    public SessionRecordMapper(
            final ClientRepository clientRepository,
            final SessionParticipantRepository participantRepository,
            final GroupSessionAttendanceRepository attendanceRepository) {
        this.clientRepository = clientRepository;
        this.participantRepository = participantRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Converts a session record entity to a response DTO.
     *
     * <p>For GROUP session records, the {@code participants} list is populated;
     * {@code clientId} and {@code clientName} are null.
     *
     * @param sessionRecord session record entity
     * @return response DTO with full session record details
     */
    public SessionRecordResponse toResponse(final SessionRecord sessionRecord) {
        if (sessionRecord == null) {
            return null;
        }

        final SessionRecordResponse.SessionTypeInfo sessionTypeInfo =
                new SessionRecordResponse.SessionTypeInfo(
                        sessionRecord.getSessionType().getId(),
                        sessionRecord.getSessionType().getCode(),
                        sessionRecord.getSessionType().getName(),
                        sessionRecord.getSessionType().getDescription()
                );

        final boolean isGroup = sessionRecord.getRecordKind() == RecordKind.GROUP;

        final String clientName;
        final List<GroupSessionParticipantResponse> participants;

        if (isGroup) {
            clientName = null;
            participants = buildParticipantList(sessionRecord.getId());
        } else {
            clientName = clientRepository.findById(sessionRecord.getClientId())
                    .map(Client::getFullName)
                    .orElse("Unknown Client");
            participants = Collections.emptyList();
        }

        return new SessionRecordResponse(
                sessionRecord.getId(),
                sessionRecord.getAppointmentId(),
                sessionRecord.getRecordKind(),
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
                sessionRecord.getAttendanceOutcome(),
                sessionRecord.getCancelledAt(),
                sessionRecord.getCancellationInitiatorId(),
                sessionRecord.getCreatedAt(),
                sessionRecord.getUpdatedAt(),
                sessionRecord.getCreatedBy(),
                participants
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<GroupSessionParticipantResponse> buildParticipantList(final UUID sessionRecordId) {
        final List<SessionParticipant> participants =
                participantRepository.findActiveBySessionRecordId(sessionRecordId);

        // Fetch all client names in one batch
        final List<UUID> clientIds = participants.stream()
                .map(SessionParticipant::getClientId)
                .toList();

        final Map<UUID, String> clientNames = clientRepository.findAllById(clientIds)
                .stream()
                .collect(Collectors.toMap(Client::getId, Client::getFullName));

        // Fetch all attendance outcomes for the session in one batch
        final Map<UUID, GroupSessionAttendance> attendanceByClient =
                attendanceRepository.findBySessionRecordId(sessionRecordId)
                        .stream()
                        .collect(Collectors.toMap(
                                GroupSessionAttendance::getClientId,
                                Function.identity()));

        return participants.stream()
                .map(p -> {
                    final GroupSessionAttendance att = attendanceByClient.get(p.getClientId());
                    return new GroupSessionParticipantResponse(
                            p.getId(),
                            p.getClientId(),
                            clientNames.getOrDefault(p.getClientId(), "Unknown Client"),
                            p.getJoinedAt(),
                            p.getRemovedAt(),
                            att != null ? att.getAttendanceOutcome() : null
                    );
                })
                .toList();
    }
}
