package com.psyassistant.reporting.reports.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL repository for the Revenue report.
 */
@Repository
public class RevenueReportRepository {

    private static final String COUNT_SQL = """
            SELECT COUNT(*)
            FROM (
                SELECT DATE_TRUNC('month', i.issued_date), tp.id
                FROM invoices i
                JOIN therapist_profile tp ON i.therapist_id = tp.user_id
                WHERE i.status NOT IN ('DRAFT', 'CANCELLED')
                  AND i.issued_date BETWEEN ? AND ?
                  AND (CAST(? AS uuid) IS NULL OR tp.id = CAST(? AS uuid))
                GROUP BY DATE_TRUNC('month', i.issued_date), tp.id
            ) sub
            """;

    private static final String DATA_SQL = """
            SELECT DATE_TRUNC('month', i.issued_date)::date  AS month,
                   tp.id                                    AS therapist_id,
                   u.full_name                              AS therapist_name,
                   SUM(i.subtotal)                          AS invoiced_total,
                   COALESCE(SUM(p.paid_sum), 0)             AS paid_total,
                   SUM(i.subtotal) - COALESCE(SUM(p.paid_sum), 0) AS outstanding_amount
            FROM invoices i
            JOIN therapist_profile tp ON i.therapist_id = tp.user_id
            JOIN users u ON tp.user_id = u.id
            LEFT JOIN (
                SELECT invoice_id, SUM(amount) AS paid_sum
                FROM payments
                GROUP BY invoice_id
            ) p ON p.invoice_id = i.id
            WHERE i.status NOT IN ('DRAFT', 'CANCELLED')
              AND i.issued_date BETWEEN ? AND ?
              AND (CAST(? AS uuid) IS NULL OR tp.id = CAST(? AS uuid))
            GROUP BY DATE_TRUNC('month', i.issued_date), tp.id, u.full_name
            ORDER BY month DESC, u.full_name
            LIMIT ? OFFSET ?
            """;

    private final JdbcTemplate jdbc;

    /**
     * Constructs the repository.
     *
     * @param jdbc JDBC template
     */
    public RevenueReportRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Counts distinct month/therapist combinations matching the filters.
     *
     * @param dateFrom    start date
     * @param dateTo      end date
     * @param therapistId optional therapist UUID string
     * @return count
     */
    public long count(final LocalDate dateFrom, final LocalDate dateTo, final String therapistId) {
        final Long total = jdbc.queryForObject(
                COUNT_SQL, Long.class, dateFrom, dateTo, therapistId, therapistId);
        return total == null ? 0L : total;
    }

    /**
     * Returns a page of revenue rows.
     *
     * @param dateFrom    start date
     * @param dateTo      end date
     * @param therapistId optional therapist UUID string
     * @param limit       max rows
     * @param offset      row offset
     * @return list of Object arrays
     */
    public List<Object[]> findPage(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String therapistId,
            final int limit,
            final int offset) {
        return jdbc.query(
                DATA_SQL,
                (rs, rowNum) -> new Object[]{
                    rs.getDate("month").toLocalDate(),
                    rs.getString("therapist_id"),
                    rs.getString("therapist_name"),
                    rs.getBigDecimal("invoiced_total"),
                    rs.getBigDecimal("paid_total"),
                    rs.getBigDecimal("outstanding_amount")
                },
                dateFrom, dateTo, therapistId, therapistId, limit, offset);
    }

    /**
     * Returns all revenue rows for export.
     *
     * @param dateFrom    start date
     * @param dateTo      end date
     * @param therapistId optional therapist UUID string
     * @return list of Object arrays
     */
    public List<Object[]> findAll(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String therapistId) {
        final String exportSql = DATA_SQL.replace("LIMIT ? OFFSET ?", "");
        return jdbc.query(
                exportSql,
                (rs, rowNum) -> new Object[]{
                    rs.getDate("month").toLocalDate(),
                    rs.getString("therapist_id"),
                    rs.getString("therapist_name"),
                    rs.getBigDecimal("invoiced_total"),
                    rs.getBigDecimal("paid_total"),
                    rs.getBigDecimal("outstanding_amount")
                },
                dateFrom, dateTo, therapistId, therapistId);
    }
}
