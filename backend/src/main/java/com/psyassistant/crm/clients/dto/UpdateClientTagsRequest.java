package com.psyassistant.crm.clients.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload for replacing all client tags.
 *
 * @param version expected optimistic-locking version
 * @param tags full replacement list of tags
 */
public record UpdateClientTagsRequest(
        @NotNull Long version,
        @NotNull @Size(max = 20) List<@Size(min = 1, max = 64) String> tags
) {
}
