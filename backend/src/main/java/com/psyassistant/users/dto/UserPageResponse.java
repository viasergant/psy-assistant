package com.psyassistant.users.dto;

import java.util.List;

/**
 * Paginated response wrapper for the user list endpoint.
 *
 * @param content       user records for the requested page
 * @param totalElements total number of users matching the current filter
 * @param totalPages    total number of pages
 * @param page          zero-based page number returned
 * @param size          page size used for this response
 */
public record UserPageResponse(
        List<UserSummaryDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
