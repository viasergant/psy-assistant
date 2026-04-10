package com.psyassistant.reporting.reports.impl;

import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.dto.RevenueRow;
import com.psyassistant.reporting.reports.repository.RevenueReportRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the Revenue report.
 */
@Service
public class RevenueReportService {

    private static final Logger LOG = LoggerFactory.getLogger(RevenueReportService.class);

    private final RevenueReportRepository repository;

    /**
     * Constructs the service.
     *
     * @param repository revenue report repository
     */
    public RevenueReportService(final RevenueReportRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns a paginated page of revenue rows.
     *
     * @param filter query filters
     * @return paged response
     */
    @Transactional(readOnly = true)
    public PagedReportResponse<RevenueRow> getPage(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;

        LOG.debug("revenue: dateFrom={} dateTo={} therapistId={}",
                filter.dateFrom(), filter.dateTo(), therapistIdStr);

        final long total = repository.count(filter.dateFrom(), filter.dateTo(), therapistIdStr);
        if (total == 0) {
            return PagedReportResponse.of(List.of(), filter.page(), filter.size(), 0L);
        }

        final List<Object[]> rows = repository.findPage(
                filter.dateFrom(), filter.dateTo(), therapistIdStr,
                filter.size(), filter.page() * filter.size());

        final List<RevenueRow> content = rows.stream().map(this::mapRow).toList();
        return PagedReportResponse.of(content, filter.page(), filter.size(), total);
    }

    /**
     * Returns all revenue rows for export.
     *
     * @param filter query filters
     * @return all rows
     */
    @Transactional(readOnly = true)
    public List<RevenueRow> getAll(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;
        return repository.findAll(filter.dateFrom(), filter.dateTo(), therapistIdStr)
                .stream().map(this::mapRow).toList();
    }

    private RevenueRow mapRow(final Object[] row) {
        return new RevenueRow(
                (LocalDate) row[0],
                row[1] != null ? UUID.fromString((String) row[1]) : null,
                (String) row[2],
                (BigDecimal) row[3],
                (BigDecimal) row[4],
                (BigDecimal) row[5]);
    }
}
