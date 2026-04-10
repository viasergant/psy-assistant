package com.psyassistant.reporting.reports.impl;

import com.psyassistant.reporting.reports.ReportProperties;
import com.psyassistant.reporting.reports.dto.ClientRetentionRow;
import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.repository.ClientRetentionReportRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the Client Retention report.
 *
 * <p>This report always returns at most one aggregate row. Churn threshold defaults
 * to {@code app.reports.churn-threshold-days} (default 90).
 */
@Service
public class ClientRetentionReportService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRetentionReportService.class);

    private final ClientRetentionReportRepository repository;
    private final ReportProperties properties;

    /**
     * Constructs the service.
     *
     * @param repository retention report repository
     * @param properties report configuration properties
     */
    public ClientRetentionReportService(
            final ClientRetentionReportRepository repository,
            final ReportProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Returns the single retention aggregate row wrapped in a paged response.
     *
     * @param filter query filters
     * @return paged response with one row (or empty if no sessions in period)
     */
    @Transactional(readOnly = true)
    public PagedReportResponse<ClientRetentionRow> getPage(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;

        LOG.debug("clientRetention: dateFrom={} dateTo={} churnDays={} therapistId={}",
                filter.dateFrom(), filter.dateTo(), properties.churnThresholdDays(), therapistIdStr);

        final List<Object[]> rows = repository.findRetentionRow(
                filter.dateFrom(), filter.dateTo(), therapistIdStr,
                properties.churnThresholdDays());

        final List<ClientRetentionRow> content = rows.stream().map(this::mapRow).toList();
        // If active_count + churned_count == 0, treat as empty
        final boolean empty = content.isEmpty() || (content.get(0).activeAtEndOfPeriod() == 0
                && content.get(0).churned() == 0 && content.get(0).newAcquired() == 0);
        if (empty) {
            return PagedReportResponse.of(List.of(), 0, filter.size(), 0L);
        }
        return PagedReportResponse.of(content, 0, 1, content.size());
    }

    /**
     * Returns all rows for export (1 row or empty).
     *
     * @param filter query filters
     * @return list with one retention row or empty
     */
    @Transactional(readOnly = true)
    public List<ClientRetentionRow> getAll(final ReportFilter filter) {
        return getPage(filter).content();
    }

    private ClientRetentionRow mapRow(final Object[] row) {
        return new ClientRetentionRow(
                (long) row[0],
                (long) row[1],
                (long) row[2],
                (BigDecimal) row[3]);
    }
}
