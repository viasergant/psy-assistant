package com.psyassistant.crm.clients.dto;

import com.psyassistant.crm.clients.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * DTO for client search results.
 * Includes all fields needed for autocomplete dropdown selection.
 */
@Schema(description = "Client search result for autocomplete and quick access")
public record ClientSearchDto(
    @Schema(description = "Client UUID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Display name (preferred name or full name)", example = "Jane Smith")
    String name,

    @Schema(description = "Client internal code", example = "CL-00123")
    String clientCode,

    @Schema(description = "Primary email", example = "jane.smith@example.com")
    String email,

    @Schema(description = "Primary phone", example = "+380671112233")
    String phone,

    @Schema(description = "Client tags", example = "[\"vip\", \"recurring\"]")
    List<String> tags
) {
    /**
     * Converts a Client entity to a search DTO.
     */
    public static ClientSearchDto from(final Client client, final List<String> tags) {
        String displayName = client.getPreferredName() != null && !client.getPreferredName().isBlank()
                ? client.getPreferredName()
                : client.getFullName();

        return new ClientSearchDto(
                client.getId(),
                displayName,
                client.getClientCode(),
                client.getEmail(),
                client.getPhone(),
                tags
        );
    }
}
