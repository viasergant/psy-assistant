package com.psyassistant.reporting.reports.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL repository for the Client Retention report.
 *
 * <p>The retention report returns a single aggregate row (not paginated per se),
 * but we model it consistently with the other repositories.
 */
@Repository
public class ClientRetentionReportRepository {

    @SuppressWarnings("checkstyle:OperatorWrap")
    private static final String DATA_SQL = """
            WITH client_last_session AS (
                SELECT client_id, MAX(session_date) AS last_session
                FROM session_record
                GROUP BY client_id
            ),
            period_active AS (
                SELECT COUNT(DISTINCT sr.client_id) AS active_count
                FROM session_record sr
                WHERE sr.session_date BETWEEN ? AND ?
                  AND (CAST(? AS uuid) IS NULL OR sr.therapist_id = CAST(? AS uuid))
            ),
            new_acquired AS (
                SELECT COUNT(DISTINCT sr.client_id) AS new_count
                FROM session_record sr
                WHERE sr.session_date BETWEEN ? AND ?
                  AND (CAST(? AS uuid) IS NULL OR sr.therapist_id = CAST(? AS uuid))
                  AND NOT EXISTS (
                      SELECT 1 FROM session_record sr2
                      WHERE sr2.client_id = sr.client_id
                        AND sr2.session_date < ?
                  )
            ),
            churned AS (
                SELECT COUNT(DISTINCT cls.client_id) AS churned_count
                FROM client_last_session cls
                WHERE cls.last_session < (? - CAST(? || ' days' AS INTERVAL))
                  AND EXISTS (
                      SELECT 1 FROM session_record sr3
                      WHERE sr3.client_id = cls.client_id
                        AND sr3.session_date < ?
                  )
            )
            SELECT pa.active_count,
                   na.new_count,
                   ch.churned_count,
                   ROUND(pa.active_count * 100.0
                       / NULLIF(pa.active_count + ch.churned_count, 0), 2) AS retention_rate
            FROM period_active pa, new_acquired na, churned ch
            """;

    private final JdbcTemplate jdbc;

    /**
     * Constructs the repository.
     *
     * @param jdbc JDBC template
     */
    public ClientRetentionReportRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Executes the client retention CTE and returns a single aggregate row.
     *
     * @param dateFrom         period start date
     * @param dateTo           period end date
     * @param therapistId      optional therapist UUID string
     * @param churnThresholdDays days of inactivity that classify a client as churned
     * @return list with one Object array: [activeCount, newCount, churnedCount, retentionRate]
     */
    public List<Object[]> findRetentionRow(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String therapistId,
            final int churnThresholdDays) {
        return jdbc.query(
                DATA_SQL,
                (rs, rowNum) -> new Object[]{
                    rs.getLong("active_count"),
                    rs.getLong("new_count"),
                    rs.getLong("churned_count"),
                    rs.getBigDecimal("retention_rate")
                },
                // period_active params
                dateFrom, dateTo, therapistId, therapistId,
                // new_acquired params
                dateFrom, dateTo, therapistId, therapistId, dateFrom,
                // churned params
                dateTo, churnThresholdDays, dateFrom);
    }
}
