package com.psyassistant.reporting.reports.impl;

import com.psyassistant.reporting.reports.dto.NoShowCancellationRow;
import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.repository.NoShowCancellationReportRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the No-Show and Cancellation Rate report.
 */
@Service
public class NoShowCancellationReportService {

    private static final Logger LOG = LoggerFactory.getLogger(NoShowCancellationReportService.class);

    private final NoShowCancellationReportRepository repository;

    /**
     * Constructs the service.
     *
     * @param repository no-show/cancellation report repository
     */
    public NoShowCancellationReportService(final NoShowCancellationReportRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns a paginated page of no-show/cancellation rows.
     *
     * @param filter query filters
     * @return paged response
     */
    @Transactional(readOnly = true)
    public PagedReportResponse<NoShowCancellationRow> getPage(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;
        final String sessionTypeIdStr = filter.sessionTypeId() != null
                ? filter.sessionTypeId().toString() : null;

        LOG.debug("noShowCancellation: dateFrom={} dateTo={} therapistId={} sessionTypeId={}",
                filter.dateFrom(), filter.dateTo(), therapistIdStr, sessionTypeIdStr);

        final long total = repository.count(
                filter.dateFrom(), filter.dateTo(), therapistIdStr, sessionTypeIdStr);
        if (total == 0) {
            return PagedReportResponse.of(List.of(), filter.page(), filter.size(), 0L);
        }

        final List<Object[]> rows = repository.findPage(
                filter.dateFrom(), filter.dateTo(), therapistIdStr, sessionTypeIdStr,
                filter.size(), filter.page() * filter.size());

        final List<NoShowCancellationRow> content = rows.stream().map(this::mapRow).toList();
        return PagedReportResponse.of(content, filter.page(), filter.size(), total);
    }

    /**
     * Returns all rows for export.
     *
     * @param filter query filters
     * @return all rows
     */
    @Transactional(readOnly = true)
    public List<NoShowCancellationRow> getAll(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;
        final String sessionTypeIdStr = filter.sessionTypeId() != null
                ? filter.sessionTypeId().toString() : null;
        return repository.findAll(
                filter.dateFrom(), filter.dateTo(), therapistIdStr, sessionTypeIdStr)
                .stream().map(this::mapRow).toList();
    }

    private NoShowCancellationRow mapRow(final Object[] row) {
        return new NoShowCancellationRow(
                row[0] != null ? UUID.fromString((String) row[0]) : null,
                (String) row[1],
                (long) row[2],
                (long) row[3],
                (BigDecimal) row[4],
                (long) row[5],
                (BigDecimal) row[6]);
    }
}
