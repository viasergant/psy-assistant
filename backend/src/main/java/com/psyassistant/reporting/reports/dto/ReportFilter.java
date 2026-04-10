package com.psyassistant.reporting.reports.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filter parameters shared by all report endpoints.
 *
 * @param dateFrom      start of the reporting period (inclusive, required)
 * @param dateTo        end of the reporting period (inclusive, required)
 * @param therapistId   optional therapist profile UUID filter
 * @param sessionTypeId optional session type UUID filter
 * @param leadSource    optional lead source string filter
 * @param page          zero-based page number (default 0)
 * @param size          page size (default 25, max 100)
 */
public record ReportFilter(
    LocalDate dateFrom,
    LocalDate dateTo,
    UUID therapistId,
    UUID sessionTypeId,
    String leadSource,
    int page,
    int size
) {
}
