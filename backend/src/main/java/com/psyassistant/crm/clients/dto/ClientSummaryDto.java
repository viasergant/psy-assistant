package com.psyassistant.crm.clients.dto;

import com.psyassistant.crm.clients.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Lightweight DTO for client selection dropdowns and lists.
 */
@Schema(description = "Lightweight client summary for dropdowns")
public record ClientSummaryDto(
    @Schema(description = "Client UUID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Display name", example = "Jane Smith")
    String name
) {
    /**
     * Converts a Client entity to a summary DTO.
     * Uses preferred name if available, otherwise full name.
     */
    public static ClientSummaryDto from(final Client client) {
        String displayName = client.getPreferredName() != null && !client.getPreferredName().isBlank()
                ? client.getPreferredName()
                : client.getFullName();

        return new ClientSummaryDto(client.getId(), displayName);
    }
}
