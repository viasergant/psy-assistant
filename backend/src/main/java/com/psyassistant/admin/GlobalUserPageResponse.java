package com.psyassistant.admin;

import java.util.List;

/**
 * Paginated response for the global administrator user list.
 *
 * @param content       user records for the requested page
 * @param totalElements total number of users matching the filter
 * @param totalPages    total number of pages
 * @param page          zero-based page number returned
 * @param size          page size used for this response
 */
public record GlobalUserPageResponse(
        List<GlobalUserSummaryDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
