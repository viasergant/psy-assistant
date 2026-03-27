package com.psyassistant.crm.leads.dto;

import java.util.List;

/**
 * Paginated list response for lead queries.
 *
 * @param content       lead summaries for the current page
 * @param totalElements total number of leads matching the current filter
 * @param totalPages    total number of pages
 * @param page          zero-based current page index
 * @param size          requested page size
 */
public record LeadPageResponse(
        List<LeadSummaryDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
