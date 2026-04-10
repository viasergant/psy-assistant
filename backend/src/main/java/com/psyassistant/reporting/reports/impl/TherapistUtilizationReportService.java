package com.psyassistant.reporting.reports.impl;

import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.dto.TherapistUtilizationRow;
import com.psyassistant.reporting.reports.repository.TherapistUtilizationReportRepository;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the Therapist Utilization report.
 */
@Service
public class TherapistUtilizationReportService {

    private static final Logger LOG = LoggerFactory.getLogger(TherapistUtilizationReportService.class);

    private final TherapistUtilizationReportRepository repository;

    /**
     * Constructs the service.
     *
     * @param repository therapist utilization report repository
     */
    public TherapistUtilizationReportService(final TherapistUtilizationReportRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns a paginated page of therapist utilization rows.
     *
     * @param filter query filters
     * @return paged response
     */
    @Transactional(readOnly = true)
    public PagedReportResponse<TherapistUtilizationRow> getPage(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;
        final long weeksInPeriod = computeWeeks(filter);

        LOG.debug("therapistUtilization: dateFrom={} dateTo={} weeks={} therapistId={}",
                filter.dateFrom(), filter.dateTo(), weeksInPeriod, therapistIdStr);

        final long total = repository.count(therapistIdStr);
        if (total == 0) {
            return PagedReportResponse.of(List.of(), filter.page(), filter.size(), 0L);
        }

        final List<Object[]> rows = repository.findPage(
                filter.dateFrom(), filter.dateTo(), weeksInPeriod, therapistIdStr,
                filter.size(), filter.page() * filter.size());

        final List<TherapistUtilizationRow> content = rows.stream().map(this::mapRow).toList();
        return PagedReportResponse.of(content, filter.page(), filter.size(), total);
    }

    /**
     * Returns all rows for export.
     *
     * @param filter query filters
     * @return all rows
     */
    @Transactional(readOnly = true)
    public List<TherapistUtilizationRow> getAll(final ReportFilter filter) {
        final String therapistIdStr = filter.therapistId() != null
                ? filter.therapistId().toString() : null;
        final long weeksInPeriod = computeWeeks(filter);
        return repository.findAll(
                filter.dateFrom(), filter.dateTo(), weeksInPeriod, therapistIdStr)
                .stream().map(this::mapRow).toList();
    }

    private long computeWeeks(final ReportFilter filter) {
        return ChronoUnit.WEEKS.between(filter.dateFrom(), filter.dateTo()) + 1;
    }

    private TherapistUtilizationRow mapRow(final Object[] row) {
        return new TherapistUtilizationRow(
                row[0] != null ? UUID.fromString((String) row[0]) : null,
                (String) row[1],
                (long) row[2],
                (Long) row[3],
                (BigDecimal) row[4]);
    }
}
