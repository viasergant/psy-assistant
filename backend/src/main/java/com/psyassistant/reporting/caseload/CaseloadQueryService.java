package com.psyassistant.reporting.caseload;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Provides native-SQL queries for the per-therapist client drill-down list.
 *
 * <p>This query deliberately excludes all clinical data fields (session notes,
 * care plan content, financial data). It exposes only scheduling-visible fields.
 */
@Service
public class CaseloadQueryService {

    private final JdbcTemplate jdbc;

    /**
     * Constructs the query service.
     *
     * @param jdbc JDBC template for native SQL execution
     */
    public CaseloadQueryService(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns a paginated list of clients currently assigned to the given therapist.
     *
     * <p>For each client, returns:
     * <ul>
     *     <li>clientId and clientName</li>
     *     <li>number of COMPLETED session records for this therapist-client pair</li>
     *     <li>earliest upcoming SCHEDULED or CONFIRMED appointment (null if none)</li>
     *     <li>client treatment_status</li>
     * </ul>
     *
     * @param therapistProfileId UUID of the therapist_profile record
     * @param pageable           pagination parameters
     * @return page of {@link TherapistClientRow}
     */
    public Page<TherapistClientRow> getClientList(
            final UUID therapistProfileId,
            final Pageable pageable) {

        final String countSql = """
                SELECT COUNT(DISTINCT c.id)
                FROM clients c
                JOIN appointment a ON a.client_id = c.id
                WHERE a.therapist_profile_id = ?
                  AND a.status NOT IN ('CANCELLED')
                """;
        final Long total = jdbc.queryForObject(countSql, Long.class, therapistProfileId);
        final long totalCount = total == null ? 0L : total;

        if (totalCount == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        final String dataSql = """
                SELECT
                    c.id                         AS client_id,
                    c.full_name                  AS client_name,
                    c.treatment_status           AS client_status,
                    COALESCE(sr.completed_count, 0) AS completed_session_count,
                    next_appt.start_time         AS next_scheduled_session
                FROM clients c
                JOIN appointment a ON a.client_id = c.id
                    AND a.therapist_profile_id = ?
                    AND a.status NOT IN ('CANCELLED')
                LEFT JOIN (
                    SELECT client_id, COUNT(*) AS completed_count
                    FROM session_record
                    WHERE therapist_id = ?
                      AND status = 'COMPLETED'
                    GROUP BY client_id
                ) sr ON sr.client_id = c.id
                LEFT JOIN LATERAL (
                    SELECT start_time
                    FROM appointment
                    WHERE client_id = c.id
                      AND therapist_profile_id = ?
                      AND status IN ('SCHEDULED', 'CONFIRMED')
                      AND start_time > NOW()
                    ORDER BY start_time ASC
                    LIMIT 1
                ) next_appt ON true
                GROUP BY c.id, c.full_name, c.treatment_status,
                         sr.completed_count, next_appt.start_time
                ORDER BY c.full_name ASC
                LIMIT ? OFFSET ?
                """;

        final List<TherapistClientRow> rows = new ArrayList<>();
        jdbc.query(
                dataSql,
                rs -> {
                    final UUID clientId = (UUID) rs.getObject("client_id");
                    final String clientName = rs.getString("client_name");
                    final String status = rs.getString("client_status");
                    final int completedCount = rs.getInt("completed_session_count");
                    final java.sql.Timestamp nextTs = rs.getTimestamp("next_scheduled_session");
                    final Instant nextSession = nextTs != null ? nextTs.toInstant() : null;
                    rows.add(new TherapistClientRow(clientId, clientName, completedCount,
                            nextSession, status));
                },
                therapistProfileId,
                therapistProfileId,
                therapistProfileId,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(rows, pageable, totalCount);
    }
}
