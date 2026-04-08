package com.psyassistant.crm.clients.dto;

import com.psyassistant.crm.clients.Client;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight client row for paginated list views.
 *
 * @param id                  client UUID
 * @param fullName            full display name
 * @param clientCode          internal client identifier
 * @param email               optional primary email
 * @param phone               optional primary phone
 * @param city                optional city
 * @param assignedTherapistId optional assigned therapist UUID
 * @param tags                sorted client tags
 * @param createdAt           record creation timestamp
 */
public record ClientListDto(
        UUID id,
        String fullName,
        String clientCode,
        String email,
        String phone,
        String city,
        UUID assignedTherapistId,
        List<String> tags,
        Instant createdAt
) {
    /**
     * Maps a {@link Client} entity and its tags to a list DTO.
     *
     * @param client the client entity
     * @param tags   sorted tag strings for this client
     * @return populated DTO
     */
    public static ClientListDto from(final Client client, final List<String> tags) {
        return new ClientListDto(
                client.getId(),
                client.getFullName(),
                client.getClientCode(),
                client.getEmail(),
                client.getPhone(),
                client.getCity(),
                client.getAssignedTherapistId(),
                tags,
                client.getCreatedAt()
        );
    }
}
