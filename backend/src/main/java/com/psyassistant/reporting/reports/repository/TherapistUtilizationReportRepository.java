package com.psyassistant.reporting.reports.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL repository for the Therapist Utilization report.
 */
@Repository
public class TherapistUtilizationReportRepository {

    private static final String COUNT_SQL = """
            SELECT COUNT(DISTINCT tp.id)
            FROM therapist_profile tp
            WHERE tp.active = true
              AND (CAST(? AS uuid) IS NULL OR tp.id = CAST(? AS uuid))
            """;

    private static final String DATA_SQL = """
            SELECT tp.id                                                          AS therapist_profile_id,
                   u.full_name                                                    AS therapist_name,
                   COALESCE(SUM(a.duration_minutes)
                       FILTER (WHERE a.status NOT IN ('CANCELLED')), 0)           AS booked_minutes,
                   CASE WHEN tp.contracted_hours_per_week IS NOT NULL
                        THEN CAST(tp.contracted_hours_per_week * ? * 60 AS BIGINT)
                        ELSE NULL END                                             AS available_minutes,
                   CASE WHEN tp.contracted_hours_per_week IS NOT NULL
                             AND tp.contracted_hours_per_week * ? * 60 > 0
                        THEN ROUND(
                            COALESCE(SUM(a.duration_minutes)
                                FILTER (WHERE a.status NOT IN ('CANCELLED')), 0)
                            * 100.0
                            / (tp.contracted_hours_per_week * ? * 60), 2)
                        ELSE NULL END                                             AS utilization_pct
            FROM therapist_profile tp
            LEFT JOIN users u ON tp.user_id = u.id
            LEFT JOIN appointment a
                   ON a.therapist_profile_id = tp.id
                  AND a.start_time BETWEEN CAST(? AS timestamptz) AND CAST(? AS timestamptz)
            WHERE tp.active = true
              AND (CAST(? AS uuid) IS NULL OR tp.id = CAST(? AS uuid))
            GROUP BY tp.id, u.full_name, tp.contracted_hours_per_week
            ORDER BY u.full_name
            LIMIT ? OFFSET ?
            """;

    private final JdbcTemplate jdbc;

    /**
     * Constructs the repository.
     *
     * @param jdbc JDBC template
     */
    public TherapistUtilizationReportRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Counts distinct active therapists matching the filter.
     *
     * @param therapistId optional therapist profile UUID (as String; pass null for all)
     * @return count
     */
    public long count(final String therapistId) {
        final Long total = jdbc.queryForObject(COUNT_SQL, Long.class, therapistId, therapistId);
        return total == null ? 0L : total;
    }

    /**
     * Returns a page of utilization rows.
     *
     * @param dateFrom     start datetime
     * @param dateTo       end datetime
     * @param weeksInPeriod number of weeks in the period for capacity calculation
     * @param therapistId  optional therapist UUID string; null means all
     * @param limit        max rows
     * @param offset       row offset
     * @return list of Object arrays
     */
    public List<Object[]> findPage(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final long weeksInPeriod,
            final String therapistId,
            final int limit,
            final int offset) {
        return jdbc.query(
                DATA_SQL,
                (rs, rowNum) -> new Object[]{
                    rs.getString("therapist_profile_id"),
                    rs.getString("therapist_name"),
                    rs.getLong("booked_minutes"),
                    (Long) (rs.getObject("available_minutes") != null
                        ? rs.getLong("available_minutes") : null),
                    rs.getBigDecimal("utilization_pct")
                },
                weeksInPeriod, weeksInPeriod, weeksInPeriod,
                dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay(),
                therapistId, therapistId,
                limit, offset);
    }

    /**
     * Returns all rows for export.
     *
     * @param dateFrom      start date
     * @param dateTo        end date
     * @param weeksInPeriod weeks in period
     * @param therapistId   optional therapist UUID string
     * @return list of Object arrays
     */
    public List<Object[]> findAll(
            final LocalDate dateFrom,
            final LocalDate dateTo,
            final long weeksInPeriod,
            final String therapistId) {
        final String exportSql = DATA_SQL.replace("LIMIT ? OFFSET ?", "");
        return jdbc.query(
                exportSql,
                (rs, rowNum) -> new Object[]{
                    rs.getString("therapist_profile_id"),
                    rs.getString("therapist_name"),
                    rs.getLong("booked_minutes"),
                    (Long) (rs.getObject("available_minutes") != null
                        ? rs.getLong("available_minutes") : null),
                    rs.getBigDecimal("utilization_pct")
                },
                weeksInPeriod, weeksInPeriod, weeksInPeriod,
                dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay(),
                therapistId, therapistId);
    }
}
