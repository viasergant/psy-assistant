package com.psyassistant.reporting.reports.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL repository for the Lead Conversion Rate report.
 */
@Repository
public class LeadConversionReportRepository {

    private static final String COUNT_SQL = """
            SELECT COUNT(DISTINCT l.source)
            FROM leads l
            WHERE l.created_at::date BETWEEN ? AND ?
              AND (? IS NULL OR l.source = ?)
            """;

    private static final String DATA_SQL = """
            SELECT l.source                                               AS lead_source,
                   COUNT(*)                                               AS total_leads,
                   COUNT(*) FILTER (WHERE l.converted_client_id IS NOT NULL) AS converted_leads,
                   ROUND(
                       COUNT(*) FILTER (WHERE l.converted_client_id IS NOT NULL)
                       * 100.0 / NULLIF(COUNT(*), 0), 2)                 AS conversion_rate
            FROM leads l
            WHERE l.created_at::date BETWEEN ? AND ?
              AND (? IS NULL OR l.source = ?)
            GROUP BY l.source
            ORDER BY l.source
            LIMIT ? OFFSET ?
            """;

    private final JdbcTemplate jdbc;

    /**
     * Constructs the repository.
     *
     * @param jdbc JDBC template for native SQL execution
     */
    public LeadConversionReportRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the total count of distinct sources matching the filters.
     *
     * @param dateFrom   start date
     * @param dateTo     end date
     * @param leadSource optional source filter
     * @return total row count
     */
    public long count(final LocalDate dateFrom, final LocalDate dateTo, final String leadSource) {
        final Long total = jdbc.queryForObject(
                COUNT_SQL, Long.class,
                dateFrom, dateTo, leadSource, leadSource);
        return total == null ? 0L : total;
    }

    /**
     * Returns a page of lead conversion rows.
     *
     * @param dateFrom   start date (inclusive)
     * @param dateTo     end date (inclusive)
     * @param leadSource optional source filter; null means all sources
     * @param limit      maximum rows to return
     * @param offset     zero-based row offset
     * @return list of Object arrays: [leadSource, totalLeads, convertedLeads, conversionRate]
     */
    public List<Object[]> findPage(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String leadSource,
            final int limit,
            final int offset) {
        return jdbc.query(
                DATA_SQL,
                (rs, rowNum) -> new Object[]{
                    rs.getString("lead_source"),
                    rs.getLong("total_leads"),
                    rs.getLong("converted_leads"),
                    rs.getBigDecimal("conversion_rate")
                },
                dateFrom, dateTo, leadSource, leadSource, limit, offset);
    }

    /**
     * Returns all lead conversion rows (for export, no pagination).
     *
     * @param dateFrom   start date (inclusive)
     * @param dateTo     end date (inclusive)
     * @param leadSource optional source filter; null means all sources
     * @return list of Object arrays
     */
    public List<Object[]> findAll(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String leadSource) {
        final String exportSql = """
                SELECT l.source                                               AS lead_source,
                       COUNT(*)                                               AS total_leads,
                       COUNT(*) FILTER (WHERE l.converted_client_id IS NOT NULL) AS converted_leads,
                       ROUND(
                           COUNT(*) FILTER (WHERE l.converted_client_id IS NOT NULL)
                           * 100.0 / NULLIF(COUNT(*), 0), 2)                 AS conversion_rate
                FROM leads l
                WHERE l.created_at::date BETWEEN ? AND ?
                  AND (? IS NULL OR l.source = ?)
                GROUP BY l.source
                ORDER BY l.source
                """;
        return jdbc.query(
                exportSql,
                (rs, rowNum) -> new Object[]{
                    rs.getString("lead_source"),
                    rs.getLong("total_leads"),
                    rs.getLong("converted_leads"),
                    rs.getBigDecimal("conversion_rate")
                },
                dateFrom, dateTo, leadSource, leadSource);
    }
}
