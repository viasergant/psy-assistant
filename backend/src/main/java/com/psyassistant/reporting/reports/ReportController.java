package com.psyassistant.reporting.reports;

import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.export.ReportExportService;
import com.psyassistant.reporting.reports.impl.ClientRetentionReportService;
import com.psyassistant.reporting.reports.impl.LeadConversionReportService;
import com.psyassistant.reporting.reports.impl.NoShowCancellationReportService;
import com.psyassistant.reporting.reports.impl.RevenueReportService;
import com.psyassistant.reporting.reports.impl.TherapistUtilizationReportService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * REST controller for the 5 operational/financial reports (PA-55).
 *
 * <p>All endpoints require {@code READ_REPORTS} or {@code READ_FINANCIAL_REPORTS} authority.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/reports/{type}/data} — paginated report rows</li>
 *   <li>{@code GET /api/v1/reports/{type}/export} — streamed download (CSV or XLSX)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final String REPORT_ACCESS = "hasAnyAuthority('READ_REPORTS','READ_FINANCIAL_REPORTS')";
    private static final DateTimeFormatter FILENAME_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LeadConversionReportService leadConversionService;
    private final TherapistUtilizationReportService utilizationService;
    private final RevenueReportService revenueService;
    private final ClientRetentionReportService retentionService;
    private final NoShowCancellationReportService noShowService;
    private final ReportExportService exportService;

    /**
     * Constructs the controller with all required services.
     *
     * @param leadConversionService  lead conversion report service
     * @param utilizationService     therapist utilization service
     * @param revenueService         revenue service
     * @param retentionService       client retention service
     * @param noShowService          no-show/cancellation service
     * @param exportService          export orchestration service
     */
    public ReportController(
            final LeadConversionReportService leadConversionService,
            final TherapistUtilizationReportService utilizationService,
            final RevenueReportService revenueService,
            final ClientRetentionReportService retentionService,
            final NoShowCancellationReportService noShowService,
            final ReportExportService exportService) {
        this.leadConversionService = leadConversionService;
        this.utilizationService = utilizationService;
        this.revenueService = revenueService;
        this.retentionService = retentionService;
        this.noShowService = noShowService;
        this.exportService = exportService;
    }

    // ---- Data endpoints -------------------------------------------------

    /**
     * Returns a paginated result set for any of the 5 report types.
     *
     * @param type          report type path variable (case-insensitive)
     * @param dateFrom      start of reporting period (required, ISO 8601 date)
     * @param dateTo        end of reporting period (required, ISO 8601 date)
     * @param therapistId   optional therapist profile UUID filter
     * @param sessionTypeId optional session type UUID filter
     * @param leadSource    optional lead source string filter
     * @param page          zero-based page number (default 0)
     * @param size          page size (default 25, max 100)
     * @return paginated report rows
     */
    @GetMapping("/{type}/data")
    @PreAuthorize(REPORT_ACCESS)
    public PagedReportResponse<?> getData(
            @PathVariable final String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateTo,
            @RequestParam(required = false) final UUID therapistId,
            @RequestParam(required = false) final UUID sessionTypeId,
            @RequestParam(required = false) final String leadSource,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "25") final int size) {

        final ReportFilter filter = new ReportFilter(
                dateFrom, dateTo, therapistId, sessionTypeId, leadSource,
                page, Math.min(size, 100));

        return switch (parseType(type)) {
            case LEAD_CONVERSION -> leadConversionService.getPage(filter);
            case THERAPIST_UTILIZATION -> utilizationService.getPage(filter);
            case REVENUE -> revenueService.getPage(filter);
            case CLIENT_RETENTION -> retentionService.getPage(filter);
            case NO_SHOW_CANCELLATION -> noShowService.getPage(filter);
        };
    }

    // ---- Export endpoints -----------------------------------------------

    /**
     * Streams a full (all-rows) export of the given report in CSV or XLSX format.
     *
     * @param type          report type
     * @param dateFrom      start of reporting period (required, ISO 8601 date)
     * @param dateTo        end of reporting period (required, ISO 8601 date)
     * @param therapistId   optional therapist profile UUID filter
     * @param sessionTypeId optional session type UUID filter
     * @param leadSource    optional lead source string filter
     * @param format        export format: {@code csv} (default) or {@code xlsx}
     * @param response      HTTP servlet response for header manipulation
     * @return streaming response body
     * @throws IOException if streaming fails
     */
    @GetMapping("/{type}/export")
    @PreAuthorize(REPORT_ACCESS)
    public StreamingResponseBody exportReport(
            @PathVariable final String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateTo,
            @RequestParam(required = false) final UUID therapistId,
            @RequestParam(required = false) final UUID sessionTypeId,
            @RequestParam(required = false) final String leadSource,
            @RequestParam(defaultValue = "csv") final String format,
            final HttpServletResponse response) throws IOException {

        final ReportType reportType = parseType(type);
        final ReportFilter filter = new ReportFilter(
                dateFrom, dateTo, therapistId, sessionTypeId, leadSource, 0, Integer.MAX_VALUE);

        final String today = LocalDate.now().format(FILENAME_DATE);
        final String baseName = type.toLowerCase() + "-report-" + today;

        if ("xlsx".equalsIgnoreCase(format)) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + baseName + ".xlsx\"");
            return out -> exportService.exportXlsx(reportType, filter, out);
        } else {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + baseName + ".csv\"");
            return out -> exportService.exportCsv(reportType, filter, response.getWriter());
        }
    }

    // ---- Helpers --------------------------------------------------------

    /**
     * Returns the report type enum for the given path variable string.
     * Converts kebab-case path variables to SCREAMING_SNAKE_CASE enum names.
     *
     * @param type path variable (e.g. "lead-conversion" or "LEAD_CONVERSION")
     * @return ReportType enum value
     */
    static ReportType parseType(final String type) {
        try {
            return ReportType.valueOf(type.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unknown report type: " + type);
        }
    }
}
