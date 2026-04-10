package com.psyassistant.reporting.reports.dto;

import java.util.List;

/**
 * Generic paginated response wrapper for report rows.
 *
 * @param <T>          the report row type
 * @param content      the rows on the current page
 * @param page         current zero-based page number
 * @param size         page size requested
 * @param totalElements total number of rows across all pages
 * @param totalPages   total number of pages
 */
public record PagedReportResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    /**
     * Factory method that constructs a paged response.
     *
     * @param <T>           row type
     * @param content       current page rows
     * @param page          zero-based page number
     * @param size          page size
     * @param totalElements total matching rows
     * @return paged response
     */
    public static <T> PagedReportResponse<T> of(
            final List<T> content,
            final int page,
            final int size,
            final long totalElements) {
        final int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedReportResponse<>(content, page, size, totalElements, totalPages);
    }
}
