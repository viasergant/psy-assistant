package com.psyassistant.crm.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.crm.clients.dto.TimelineEventDto;
import jakarta.persistence.EntityNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for querying client activity timeline.
 * Aggregates events from appointments, profile changes, and conversion history.
 */
@Service
public class TimelineService {

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            "APPOINTMENT", "PROFILE_CHANGE", "CONVERSION", "NOTE", "PAYMENT", "COMMUNICATION",
            "ATTENDANCE_OUTCOME"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the service.
     */
    public TimelineService(final JdbcTemplate jdbcTemplate,
                           final ClientRepository clientRepository,
                           final ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves client activity timeline with optional filtering and pagination.
     *
     * @param clientId client UUID
     * @param eventTypes optional event type filter (null = all types)
     * @param page page number (0-based)
     * @param size page size (max 100)
     * @return list of timeline events in descending chronological order
     */
    @Transactional(readOnly = true)
    public List<TimelineEventDto> getClientTimeline(final UUID clientId,
                                                     final List<String> eventTypes,
                                                     final int page,
                                                     final int size) {
        // Verify client exists and user has access
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientId));

        enforceReadAccess(client);

        // Validate event types
        if (eventTypes != null) {
            for (String type : eventTypes) {
                if (!VALID_EVENT_TYPES.contains(type)) {
                    throw new IllegalArgumentException("Invalid event type: " + type);
                }
            }
        }

        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), 100);
        int offset = effectivePage * effectiveSize;

        String sql = buildTimelineQuery(eventTypes);

        RowMapper<TimelineEventDto> mapper = new TimelineEventRowMapper();

        if (eventTypes == null || eventTypes.isEmpty()) {
            return jdbcTemplate.query(sql, mapper, clientId, effectiveSize, offset);
        } else {
            Object[] params = new Object[eventTypes.size() + 3];
            params[0] = clientId;
            for (int i = 0; i < eventTypes.size(); i++) {
                params[i + 1] = eventTypes.get(i);
            }
            params[params.length - 2] = effectiveSize;
            params[params.length - 1] = offset;
            return jdbcTemplate.query(sql, mapper, params);
        }
    }

    private String buildTimelineQuery(final List<String> eventTypes) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                event_id,
                event_type,
                event_subtype,
                event_timestamp,
                actor_name,
                event_data,
                created_at
            FROM client_activity_timeline
            WHERE client_id = ?
            """);

        if (eventTypes != null && !eventTypes.isEmpty()) {
            sql.append(" AND event_type IN (");
            for (int i = 0; i < eventTypes.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
            }
            sql.append(")");
        }

        sql.append(" ORDER BY event_timestamp DESC");
        sql.append(" LIMIT ? OFFSET ?");

        return sql.toString();
    }

    private void enforceReadAccess(final Client client) {
        boolean canManageClients = hasAuthority("MANAGE_CLIENTS");
        boolean canReadAll = hasAuthority("READ_CLIENTS_ALL");
        boolean canReadAssigned = hasAuthority("READ_ASSIGNED_CLIENTS");

        if (!canManageClients && !canReadAll && !canReadAssigned) {
            throw new AccessDeniedException("Access denied");
        }

        // If user only has READ_ASSIGNED_CLIENTS, verify they're the assigned therapist
        if (!canManageClients && !canReadAll && canReadAssigned) {
            UUID actorId = currentPrincipalId();
            if (actorId == null || !Objects.equals(actorId, client.getAssignedTherapistId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
    }

    private boolean hasAuthority(final String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private UUID currentPrincipalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Row mapper for timeline events from materialized view.
     */
    private class TimelineEventRowMapper implements RowMapper<TimelineEventDto> {
        @Override
        public TimelineEventDto mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            UUID eventId = (UUID) rs.getObject("event_id");
            String eventType = rs.getString("event_type");
            String eventSubtype = rs.getString("event_subtype");

            Timestamp tsTimestamp = rs.getTimestamp("event_timestamp");
            OffsetDateTime eventTimestamp = tsTimestamp != null
                    ? OffsetDateTime.ofInstant(tsTimestamp.toInstant(), ZoneId.systemDefault())
                    : null;

            String actorName = rs.getString("actor_name");

            String eventDataJson = rs.getString("event_data");
            JsonNode eventData = null;
            if (eventDataJson != null) {
                try {
                    eventData = objectMapper.readTree(eventDataJson);
                } catch (Exception ex) {
                    // Log but don't fail
                    eventData = objectMapper.nullNode();
                }
            }

            Timestamp createdTimestamp = rs.getTimestamp("created_at");
            OffsetDateTime createdAt = createdTimestamp != null
                    ? OffsetDateTime.ofInstant(createdTimestamp.toInstant(), ZoneId.systemDefault())
                    : null;

            return new TimelineEventDto(
                    eventId,
                    eventType,
                    eventSubtype,
                    eventTimestamp,
                    actorName,
                    eventData,
                    createdAt
            );
        }
    }
}
