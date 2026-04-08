package com.psyassistant.crm.clients.dto;

import java.util.List;

/**
 * Paginated list response for client queries.
 *
 * @param content       client list rows for the current page
 * @param totalElements total number of clients matching the current filter
 * @param totalPages    total number of pages
 * @param page          zero-based current page index
 * @param size          requested page size
 */
public record ClientPageResponse(
        List<ClientListDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
