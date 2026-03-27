package com.psyassistant.crm.clients.dto;

import com.psyassistant.crm.clients.Client;
import java.time.Instant;
import java.util.UUID;

/**
 * Detail view of a single client record.
 *
 * @param id           client UUID
 * @param fullName     full display name
 * @param sourceLeadId UUID of the originating lead
 * @param ownerId      optional owner user UUID
 * @param notes        optional free-text notes
 * @param createdAt    record creation timestamp
 * @param updatedAt    record last-modified timestamp
 * @param createdBy    email/name of the principal that created the record
 */
public record ClientDetailDto(
        UUID id,
        String fullName,
        UUID sourceLeadId,
        UUID ownerId,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {

    /**
     * Builds a {@link ClientDetailDto} from a {@link Client} entity.
     *
     * @param client the client entity
     * @return the populated DTO
     */
    public static ClientDetailDto from(final Client client) {
        return new ClientDetailDto(
                client.getId(),
                client.getFullName(),
                client.getSourceLeadId(),
                client.getOwnerId(),
                client.getNotes(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getCreatedBy()
        );
    }
}
