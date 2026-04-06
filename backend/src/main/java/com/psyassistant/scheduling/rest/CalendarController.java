package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.dto.CalendarWeekViewResponse;
import com.psyassistant.scheduling.service.CalendarService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for calendar view operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *     <li>Week view with multi-therapist support</li>
 *     <li>Day view (coming in Phase 2)</li>
 *     <li>Month view (coming in Phase 2)</li>
 * </ul>
 *
 * <p><strong>Security</strong>: All endpoints require STAFF role or higher.
 */
@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarController.class);

    private final CalendarService calendarService;

    public CalendarController(final CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Fetches calendar data for week view.
     *
     * <p>Returns appointments for the specified week (Monday-Sunday) with therapist metadata.
     * Supports multi-therapist filtering for side-by-side column display.
     *
     * <p><strong>Query Parameters</strong>:
     * <ul>
     *     <li>{@code weekDate}: Any date within the target week (ISO 8601 format: YYYY-MM-DD).
     *         Defaults to current week if not provided.</li>
     *     <li>{@code therapistIds}: Optional comma-separated list of therapist profile UUIDs to filter.
     *         If not provided, returns appointments for all therapists.</li>
     *     <li>{@code timezone}: IANA timezone identifier for date calculations (e.g., "America/New_York").
     *         Defaults to "UTC" if not provided.</li>
     * </ul>
     *
     * <p><strong>Example Request</strong>:
     * <pre>
     * GET /api/v1/calendar/week?weekDate=2026-04-06&therapistIds=uuid1,uuid2&timezone=America/New_York
     * </pre>
     *
     * <p><strong>Example Response</strong>:
     * <pre>{@code
     * {
     *   "weekStart": "2026-04-06",
     *   "weekEnd": "2026-04-13",
     *   "therapists": {
     *     "uuid1": {
     *       "id": "uuid1",
     *       "name": "Dr. Jane Smith",
     *       "specialization": "Cognitive Behavioral Therapy"
     *     }
     *   },
     *   "appointments": [
     *     {
     *       "id": "appt-uuid",
     *       "therapistProfileId": "uuid1",
     *       "therapistName": "Dr. Jane Smith",
     *       "clientId": "client-uuid",
     *       "clientName": "John Doe",
     *       "sessionTypeCode": "CBT_INDIVIDUAL",
     *       "sessionTypeName": "CBT Individual Session",
     *       "startTime": "2026-04-07T10:00:00-04:00",
     *       "endTime": "2026-04-07T11:00:00-04:00",
     *       "durationMinutes": 60,
     *       "status": "CONFIRMED",
     *       "isModified": false,
     *       "notes": null
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @param weekDate any date within the target week (defaults to today)
     * @param therapistIds optional list of therapist IDs to filter
     * @param timezone timezone for date calculations (defaults to UTC)
     * @return week view with appointments and therapist metadata (200 OK)
     */
    @GetMapping("/week")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<CalendarWeekViewResponse> getWeekView(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate weekDate,
            @RequestParam(required = false) final List<UUID> therapistIds,
            @RequestParam(required = false, defaultValue = "UTC") final String timezone) {

        LOG.debug("GET /api/v1/calendar/week: weekDate={}, therapistIds={}, timezone={}",
                weekDate, therapistIds, timezone);

        final CalendarWeekViewResponse response = calendarService.getWeekView(weekDate, therapistIds, timezone);

        LOG.debug("Returning {} appointments for {} therapists",
                response.appointments().size(), response.therapists().size());

        return ResponseEntity.ok(response);
    }
}
