package com.psyassistant.reporting.caseload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job that computes daily per-therapist caseload metrics and upserts
 * them into the {@code caseload_snapshot} table.
 *
 * <p>Runs once per day (default: 02:00 UTC). Re-running on the same day is safe
 * because the SQL uses {@code ON CONFLICT ... DO UPDATE}.
 *
 * <p>Disabled when {@code app.caseload.job-enabled=false} (useful for tests).
 */
@Component
@ConditionalOnProperty(name = "app.caseload.job-enabled", matchIfMissing = true)
public class CaseloadSnapshotJob {

    private static final Logger LOG = LoggerFactory.getLogger(CaseloadSnapshotJob.class);
    private static final int MINUTES_PER_HOUR = 60;
    private static final int UTILIZATION_SCALE = 4;

    private final JdbcTemplate jdbc;
    private final CaseloadProperties props;

    /**
     * Constructs the job with its dependencies.
     *
     * @param jdbc  JDBC template for low-level aggregation queries
     * @param props caseload configuration properties
     */
    public CaseloadSnapshotJob(final JdbcTemplate jdbc, final CaseloadProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    /**
     * Executes the daily caseload snapshot aggregation.
     *
     * <p>Fetches all active therapists, computes their metrics for today,
     * and upserts one snapshot row per therapist.
     */
    @Scheduled(cron = "${app.caseload.job-cron}")
    @Transactional
    public void runDailySnapshot() {
        final LocalDate today = LocalDate.now();
        LOG.info("CaseloadSnapshotJob starting for snapshot_date={}", today);

        final List<Map<String, Object>> therapists = fetchActiveTherapists();
        int processed = 0;

        for (Map<String, Object> row : therapists) {
            final UUID therapistId = (UUID) row.get("id");
            final BigDecimal contracted = (BigDecimal) row.get("contracted_hours_per_week");
            try {
                final int activeClients = countActiveClients(therapistId, today);
                final int sessionsWeek = countSessionsThisWeek(therapistId, today);
                final int sessionsMonth = countSessionsThisMonth(therapistId, today);
                final BigDecimal hoursWeek = scheduledHoursThisWeek(therapistId, today);
                final BigDecimal utilization = computeUtilization(hoursWeek, contracted);

                upsertSnapshot(therapistId, today, activeClients, sessionsWeek,
                        sessionsMonth, hoursWeek, contracted, utilization);
                processed++;
            } catch (Exception ex) {
                LOG.error("CaseloadSnapshotJob failed for therapistId={}: {}",
                        therapistId, ex.getMessage(), ex);
            }
        }

        LOG.info("CaseloadSnapshotJob finished: processed={} therapists for date={}",
                processed, today);
    }

    // ---- private helpers -----------------------------------------------

    private List<Map<String, Object>> fetchActiveTherapists() {
        return jdbc.queryForList(
                "SELECT id, contracted_hours_per_week FROM therapist_profile WHERE active = true");
    }

    private int countActiveClients(final UUID therapistId, final LocalDate today) {
        final String sql = """
                SELECT COUNT(DISTINCT a.client_id)
                FROM appointment a
                WHERE a.therapist_profile_id = ?
                  AND a.status = 'COMPLETED'
                  AND a.start_time >= NOW() - (? || ' days')::interval
                  AND a.start_time < NOW()
                """;
        final Long count = jdbc.queryForObject(sql, Long.class,
                therapistId, props.activeClientDays());
        return count == null ? 0 : count.intValue();
    }

    private int countSessionsThisWeek(final UUID therapistId, final LocalDate today) {
        final int week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        final int year = today.get(IsoFields.WEEK_BASED_YEAR);
        final String sql = """
                SELECT COUNT(*)
                FROM appointment a
                WHERE a.therapist_profile_id = ?
                  AND a.status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED')
                  AND EXTRACT(ISOYEAR FROM a.start_time) = ?
                  AND EXTRACT(WEEK FROM a.start_time) = ?
                """;
        final Long count = jdbc.queryForObject(sql, Long.class, therapistId, year, week);
        return count == null ? 0 : count.intValue();
    }

    private int countSessionsThisMonth(final UUID therapistId, final LocalDate today) {
        final String sql = """
                SELECT COUNT(*)
                FROM appointment a
                WHERE a.therapist_profile_id = ?
                  AND a.status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED')
                  AND EXTRACT(YEAR FROM a.start_time) = ?
                  AND EXTRACT(MONTH FROM a.start_time) = ?
                """;
        final Long count = jdbc.queryForObject(sql, Long.class,
                therapistId, today.getYear(), today.getMonthValue());
        return count == null ? 0 : count.intValue();
    }

    private BigDecimal scheduledHoursThisWeek(final UUID therapistId, final LocalDate today) {
        final int week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        final int year = today.get(IsoFields.WEEK_BASED_YEAR);
        final String sql = """
                SELECT COALESCE(SUM(a.duration_minutes), 0)
                FROM appointment a
                WHERE a.therapist_profile_id = ?
                  AND a.status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED')
                  AND EXTRACT(ISOYEAR FROM a.start_time) = ?
                  AND EXTRACT(WEEK FROM a.start_time) = ?
                """;
        final Integer totalMinutes = jdbc.queryForObject(sql, Integer.class, therapistId, year, week);
        final int minutes = totalMinutes == null ? 0 : totalMinutes;
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), UTILIZATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal computeUtilization(
            final BigDecimal scheduledHours,
            final BigDecimal contractedHours) {
        if (contractedHours == null || contractedHours.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return scheduledHours.divide(contractedHours, UTILIZATION_SCALE, RoundingMode.HALF_UP);
    }

    private void upsertSnapshot(
            final UUID therapistId,
            final LocalDate today,
            final int activeClients,
            final int sessionsWeek,
            final int sessionsMonth,
            final BigDecimal hoursWeek,
            final BigDecimal contracted,
            final BigDecimal utilization) {
        final String sql = """
                INSERT INTO caseload_snapshot
                    (id, therapist_profile_id, snapshot_date, active_client_count,
                     sessions_this_week, sessions_this_month, scheduled_hours_this_week,
                     contracted_hours_per_week, utilization_rate, created_at, updated_at)
                VALUES
                    (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (therapist_profile_id, snapshot_date)
                DO UPDATE SET
                    active_client_count       = EXCLUDED.active_client_count,
                    sessions_this_week        = EXCLUDED.sessions_this_week,
                    sessions_this_month       = EXCLUDED.sessions_this_month,
                    scheduled_hours_this_week = EXCLUDED.scheduled_hours_this_week,
                    contracted_hours_per_week = EXCLUDED.contracted_hours_per_week,
                    utilization_rate          = EXCLUDED.utilization_rate,
                    updated_at                = NOW()
                """;
        jdbc.update(sql, therapistId, today, activeClients, sessionsWeek,
                sessionsMonth, hoursWeek, contracted, utilization);
    }
}
