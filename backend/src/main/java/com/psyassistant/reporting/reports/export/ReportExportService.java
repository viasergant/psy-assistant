package com.psyassistant.reporting.reports.export;

import com.psyassistant.reporting.reports.ReportProperties;
import com.psyassistant.reporting.reports.ReportType;
import com.psyassistant.reporting.reports.dto.ClientRetentionRow;
import com.psyassistant.reporting.reports.dto.LeadConversionRow;
import com.psyassistant.reporting.reports.dto.NoShowCancellationRow;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.dto.RevenueRow;
import com.psyassistant.reporting.reports.dto.TherapistUtilizationRow;
import com.psyassistant.reporting.reports.impl.ClientRetentionReportService;
import com.psyassistant.reporting.reports.impl.LeadConversionReportService;
import com.psyassistant.reporting.reports.impl.NoShowCancellationReportService;
import com.psyassistant.reporting.reports.impl.RevenueReportService;
import com.psyassistant.reporting.reports.impl.TherapistUtilizationReportService;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orchestrates report data retrieval and streaming export in CSV or XLSX format.
 *
 * <p>Export is capped at {@code app.reports.export-row-cap} rows. Attempts to export
 * more than the cap result in a 400 Bad Request.
 */
@Service
public class ReportExportService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportExportService.class);

    private final LeadConversionReportService leadConversionService;
    private final TherapistUtilizationReportService utilizationService;
    private final RevenueReportService revenueService;
    private final ClientRetentionReportService retentionService;
    private final NoShowCancellationReportService noShowService;
    private final ReportProperties properties;
    private final CsvExportWriter csvWriter;
    private final XlsxExportWriter xlsxWriter;

    /**
     * Constructs the export service with all required dependencies.
     *
     * @param leadConversionService  lead conversion service
     * @param utilizationService     therapist utilization service
     * @param revenueService         revenue service
     * @param retentionService       client retention service
     * @param noShowService          no-show/cancellation service
     * @param properties             report configuration
     */
    public ReportExportService(
            final LeadConversionReportService leadConversionService,
            final TherapistUtilizationReportService utilizationService,
            final RevenueReportService revenueService,
            final ClientRetentionReportService retentionService,
            final NoShowCancellationReportService noShowService,
            final ReportProperties properties) {
        this.leadConversionService = leadConversionService;
        this.utilizationService = utilizationService;
        this.revenueService = revenueService;
        this.retentionService = retentionService;
        this.noShowService = noShowService;
        this.properties = properties;
        this.csvWriter = new CsvExportWriter();
        this.xlsxWriter = new XlsxExportWriter();
    }

    /**
     * Streams a CSV export to the given writer.
     *
     * @param type   report type
     * @param filter query filters
     * @param writer output writer
     * @throws IOException if streaming fails
     */
    public void exportCsv(final ReportType type, final ReportFilter filter, final PrintWriter writer)
            throws IOException {
        final ExportData data = loadAndValidate(type, filter);
        csvWriter.write(writer, data.headers(), data.rows());
        LOG.info("CSV export completed: type={} rows={}", type, data.rows().size());
    }

    /**
     * Streams an XLSX export to the given output stream.
     *
     * @param type   report type
     * @param filter query filters
     * @param out    output stream
     * @throws IOException if streaming fails
     */
    public void exportXlsx(final ReportType type, final ReportFilter filter, final OutputStream out)
            throws IOException {
        final ExportData data = loadAndValidate(type, filter);
        xlsxWriter.write(out, data.headers(), data.rows());
        LOG.info("XLSX export completed: type={} rows={}", type, data.rows().size());
    }

    private ExportData loadAndValidate(final ReportType type, final ReportFilter filter) {
        final ExportData data = buildExportData(type, filter);
        if (data.rows().size() > properties.exportRowCap()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Export exceeds row cap of " + properties.exportRowCap());
        }
        return data;
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private ExportData buildExportData(final ReportType type, final ReportFilter filter) {
        return switch (type) {
            case LEAD_CONVERSION -> {
                final List<LeadConversionRow> rows = leadConversionService.getAll(filter);
                yield new ExportData(
                        List.of("Lead Source", "Total Leads", "Converted", "Conversion Rate %"),
                        rows.stream().map(r -> new Object[]{
                            r.leadSource(), r.totalLeads(), r.convertedLeads(), r.conversionRate()
                        }).toList());
            }
            case THERAPIST_UTILIZATION -> {
                final List<TherapistUtilizationRow> rows = utilizationService.getAll(filter);
                yield new ExportData(
                        List.of("Therapist", "Booked Minutes", "Available Minutes", "Utilization %"),
                        rows.stream().map(r -> new Object[]{
                            r.therapistName(), r.bookedMinutes(), r.availableMinutes(), r.utilizationPct()
                        }).toList());
            }
            case REVENUE -> {
                final List<RevenueRow> rows = revenueService.getAll(filter);
                yield new ExportData(
                        List.of("Month", "Therapist", "Invoiced", "Paid", "Outstanding"),
                        rows.stream().map(r -> new Object[]{
                            r.month(), r.therapistName(), r.invoicedTotal(), r.paidTotal(), r.outstandingAmount()
                        }).toList());
            }
            case CLIENT_RETENTION -> {
                final List<ClientRetentionRow> rows = retentionService.getAll(filter);
                yield new ExportData(
                        List.of("Active Clients", "New Clients", "Churned", "Retention Rate %"),
                        rows.stream().map(r -> new Object[]{
                            r.activeAtEndOfPeriod(), r.newAcquired(), r.churned(), r.retentionRate()
                        }).toList());
            }
            case NO_SHOW_CANCELLATION -> {
                final List<NoShowCancellationRow> rows = noShowService.getAll(filter);
                yield new ExportData(
                        List.of("Therapist", "Total Scheduled", "No-Shows", "No-Show Rate %",
                                "Cancellations", "Cancellation Rate %"),
                        rows.stream().map(r -> new Object[]{
                            r.therapistName(), r.totalScheduled(), r.noShowCount(), r.noShowRate(),
                            r.cancellationCount(), r.cancellationRate()
                        }).toList());
            }
        };
    }

    private record ExportData(List<String> headers, List<Object[]> rows) {
    }
}
