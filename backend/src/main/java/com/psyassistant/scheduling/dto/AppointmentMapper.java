package com.psyassistant.scheduling.dto;

import com.psyassistant.riskflags.domain.ClientRiskFlag;
import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import com.psyassistant.riskflags.domain.RiskFlagType;
import com.psyassistant.riskflags.repository.ClientRiskFlagRepository;
import com.psyassistant.riskflags.repository.RiskFlagTypeRepository;
import com.psyassistant.scheduling.domain.Appointment;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link Appointment} entities and DTOs.
 *
 * <p>Centralizes DTO mapping logic to avoid duplication across controllers and services.
 */
@Component
public class AppointmentMapper {

    private final ClientRiskFlagRepository clientRiskFlagRepository;
    private final RiskFlagTypeRepository riskFlagTypeRepository;

    public AppointmentMapper(final ClientRiskFlagRepository clientRiskFlagRepository,
                             final RiskFlagTypeRepository riskFlagTypeRepository) {
        this.clientRiskFlagRepository = clientRiskFlagRepository;
        this.riskFlagTypeRepository = riskFlagTypeRepository;
    }

    /**
     * Converts an appointment entity to a slim response DTO without risk flag enrichment.
     *
     * <p>Intended for list endpoints (e.g. therapist calendar, series) where fetching risk
     * flags per appointment would cause N+1 queries. The {@code activeRiskFlagTypes} field
     * is always empty in this representation.
     *
     * @param appointment appointment entity
     * @return response DTO with base appointment details and empty risk flag list
     */
    public AppointmentResponse toResponse(final Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return buildResponse(appointment, List.of());
    }

    /**
     * Converts an appointment entity to a fully-enriched response DTO for the detail view.
     *
     * <p>Includes active risk flag type names for the client, fetched in a single
     * batch query via {@code RiskFlagTypeRepository.findAllById}. Use this only for
     * single-appointment detail endpoints to avoid N+1 queries on list endpoints.
     *
     * @param appointment appointment entity
     * @return response DTO with full appointment details including active risk flag types
     */
    public AppointmentResponse toDetailResponse(final Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        final List<String> activeRiskFlagTypes = resolveActiveRiskFlagTypeNames(appointment.getClientId());

        return buildResponse(appointment, activeRiskFlagTypes);
    }

    private AppointmentResponse buildResponse(final Appointment appointment,
                                               final List<String> activeRiskFlagTypes) {
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
                appointment.isModified(),
                activeRiskFlagTypes
        );
    }

    /**
     * Resolves active risk flag type names for the given client.
     *
     * <p>Uses two queries: one to fetch active flags by client, one batch lookup
     * of all distinct flag type IDs — avoiding N+1.
     *
     * @param clientId client whose active flags are queried
     * @return list of flag type names; empty if client has no active flags
     */
    private List<String> resolveActiveRiskFlagTypeNames(final UUID clientId) {
        final List<ClientRiskFlag> activeFlags =
                clientRiskFlagRepository.findAllByClientIdAndStatus(clientId, ClientRiskFlagStatus.ACTIVE);

        if (activeFlags.isEmpty()) {
            return List.of();
        }

        final Set<UUID> flagTypeIds = activeFlags.stream()
                .map(ClientRiskFlag::getFlagTypeId)
                .collect(Collectors.toSet());

        final Map<UUID, String> typeNamesById = riskFlagTypeRepository.findAllById(flagTypeIds).stream()
                .collect(Collectors.toMap(RiskFlagType::getId, RiskFlagType::getName));

        return activeFlags.stream()
                .map(flag -> typeNamesById.get(flag.getFlagTypeId()))
                .filter(name -> name != null)
                .distinct()
                .collect(Collectors.toList());
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
