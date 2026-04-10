package com.psyassistant.reporting.reports.impl;

import com.psyassistant.reporting.reports.dto.LeadConversionRow;
import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.repository.LeadConversionReportRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the Lead Conversion Rate report.
 */
@Service
public class LeadConversionReportService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadConversionReportService.class);

    private final LeadConversionReportRepository repository;

    /**
     * Constructs the service.
     *
     * @param repository lead conversion report repository
     */
    public LeadConversionReportService(final LeadConversionReportRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns a paginated page of lead conversion rows.
     *
     * @param filter query filters
     * @return paged response
     */
    @Transactional(readOnly = true)
    public PagedReportResponse<LeadConversionRow> getPage(final ReportFilter filter) {
        LOG.debug("leadConversion: dateFrom={} dateTo={} source={} page={} size={}",
                filter.dateFrom(), filter.dateTo(), filter.leadSource(), filter.page(), filter.size());

        final long total = repository.count(filter.dateFrom(), filter.dateTo(), filter.leadSource());
        if (total == 0) {
            return PagedReportResponse.of(List.of(), filter.page(), filter.size(), 0L);
        }

        final List<Object[]> rows = repository.findPage(
                filter.dateFrom(), filter.dateTo(), filter.leadSource(),
                filter.size(), filter.page() * filter.size());

        final List<LeadConversionRow> content = rows.stream()
                .map(this::mapRow)
                .toList();
        return PagedReportResponse.of(content, filter.page(), filter.size(), total);
    }

    /**
     * Returns all lead conversion rows for export.
     *
     * @param filter query filters
     * @return all rows
     */
    @Transactional(readOnly = true)
    public List<LeadConversionRow> getAll(final ReportFilter filter) {
        return repository.findAll(filter.dateFrom(), filter.dateTo(), filter.leadSource())
                .stream().map(this::mapRow).toList();
    }

    private LeadConversionRow mapRow(final Object[] row) {
        return new LeadConversionRow(
                (String) row[0],
                (long) row[1],
                (long) row[2],
                (BigDecimal) row[3]);
    }
}
