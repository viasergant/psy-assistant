package com.psyassistant.reporting.reports.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL repository for the No-Show and Cancellation Rate report.
 */
@Repository
public class NoShowCancellationReportRepository {

    private static final String COUNT_SQL = """
            SELECT COUNT(DISTINCT tp.id)
            FROM appointment a
            JOIN therapist_profile tp ON a.therapist_profile_id = tp.id
            WHERE a.start_time BETWEEN CAST(? AS timestamptz) AND CAST(? AS timestamptz)
              AND (CAST(? AS uuid) IS NULL OR tp.id = CAST(? AS uuid))
              AND (CAST(? AS uuid) IS NULL OR a.session_type_id = CAST(? AS uuid))
            """;

    private static final String DATA_SQL = """
            SELECT tp.id                                                          AS therapist_id,
                   u.full_name                                                    AS therapist_name,
                   COUNT(*)                                                       AS total_scheduled,
                   COUNT(*) FILTER (WHERE a.status = 'NO_SHOW')                  AS no_show_count,
                   ROUND(COUNT(*) FILTER (WHERE a.status = 'NO_SHOW')
                       * 100.0 / NULLIF(COUNT(*), 0), 2)                         AS no_show_rate,
                   COUNT(*) FILTER (WHERE a.status = 'CANCELLED')                AS cancellation_count,
                   ROUND(COUNT(*) FILTER (WHERE a.status = 'CANCELLED')
                       * 100.0 / NULLIF(COUNT(*), 0), 2)                         AS cancellation_rate
            FROM appointment a
            JOIN therapist_profile tp ON a.therapist_profile_id = tp.id
            JOIN users u ON tp.user_id = u.id
            WHERE a.start_time BETWEEN CAST(? AS timestamptz) AND CAST(? AS timestamptz)
              AND (CAST(? AS uuid) IS NULL OR tp.id = CAST(? AS uuid))
              AND (CAST(? AS uuid) IS NULL OR a.session_type_id = CAST(? AS uuid))
            GROUP BY tp.id, u.full_name
            ORDER BY u.full_name
            LIMIT ? OFFSET ?
            """;

    private final JdbcTemplate jdbc;

    /**
     * Constructs the repository.
     *
     * @param jdbc JDBC template
     */
    public NoShowCancellationReportRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Counts distinct therapists with appointments in the period.
     *
     * @param dateFrom      start date
     * @param dateTo        end date
     * @param therapistId   optional therapist UUID string
     * @param sessionTypeId optional session type UUID string
     * @return count
     */
    public long count(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String therapistId,
            final String sessionTypeId) {
        final Long total = jdbc.queryForObject(
                COUNT_SQL, Long.class,
                dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay(),
                therapistId, therapistId, sessionTypeId, sessionTypeId);
        return total == null ? 0L : total;
    }

    /**
     * Returns a page of no-show/cancellation rows.
     *
     * @param dateFrom      start date
     * @param dateTo        end date
     * @param therapistId   optional therapist UUID string
     * @param sessionTypeId optional session type UUID string
     * @param limit         max rows
     * @param offset        row offset
     * @return list of Object arrays
     */
    public List<Object[]> findPage(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String therapistId,
            final String sessionTypeId,
            final int limit,
            final int offset) {
        return jdbc.query(
                DATA_SQL,
                (rs, rowNum) -> new Object[]{
                    rs.getString("therapist_id"),
                    rs.getString("therapist_name"),
                    rs.getLong("total_scheduled"),
                    rs.getLong("no_show_count"),
                    rs.getBigDecimal("no_show_rate"),
                    rs.getLong("cancellation_count"),
                    rs.getBigDecimal("cancellation_rate")
                },
                dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay(),
                therapistId, therapistId, sessionTypeId, sessionTypeId,
                limit, offset);
    }

    /**
     * Returns all rows for export.
     *
     * @param dateFrom      start date
     * @param dateTo        end date
     * @param therapistId   optional therapist UUID string
     * @param sessionTypeId optional session type UUID string
     * @return list of Object arrays
     */
    public List<Object[]> findAll(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final String therapistId,
            final String sessionTypeId) {
        final String exportSql = DATA_SQL.replace("LIMIT ? OFFSET ?", "");
        return jdbc.query(
                exportSql,
                (rs, rowNum) -> new Object[]{
                    rs.getString("therapist_id"),
                    rs.getString("therapist_name"),
                    rs.getLong("total_scheduled"),
                    rs.getLong("no_show_count"),
                    rs.getBigDecimal("no_show_rate"),
                    rs.getLong("cancellation_count"),
                    rs.getBigDecimal("cancellation_rate")
                },
                dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay(),
                therapistId, therapistId, sessionTypeId, sessionTypeId);
    }
}
