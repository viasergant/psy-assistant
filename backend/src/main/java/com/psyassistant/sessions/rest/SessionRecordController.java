package com.psyassistant.sessions.rest;

import com.psyassistant.sessions.domain.SessionParticipant;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.dto.AttendanceOutcomeResponse;
import com.psyassistant.sessions.dto.CancelSessionRequest;
import com.psyassistant.sessions.dto.CompleteSessionRequest;
import com.psyassistant.sessions.dto.GroupSessionParticipantResponse;
import com.psyassistant.sessions.dto.RecordAttendanceRequest;
import com.psyassistant.sessions.dto.RecordGroupAttendanceRequest;
import com.psyassistant.sessions.dto.SessionRecordMapper;
import com.psyassistant.sessions.dto.SessionRecordResponse;
import com.psyassistant.sessions.dto.StartSessionRequest;
import com.psyassistant.sessions.service.AttendanceOutcomeService;
import com.psyassistant.sessions.service.GroupAttendanceService;
import com.psyassistant.sessions.service.GroupSessionRecordService;
import com.psyassistant.sessions.service.SessionRecordService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for session record management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *     <li>Manual session start from appointments</li>
 *     <li>Retrieving session records</li>
 * </ul>
 *
 * <p>Authorization: Requires authenticated therapist, admin staff, or system administrator roles.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionRecordController {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRecordController.class);

    private final SessionRecordService sessionRecordService;
    private final SessionRecordMapper sessionRecordMapper;
    private final AttendanceOutcomeService attendanceOutcomeService;
    private final GroupSessionRecordService groupSessionRecordService;
    private final GroupAttendanceService groupAttendanceService;

    /**
     * Constructs a new SessionRecordController.
     *
     * @param sessionRecordService       the session record service
     * @param sessionRecordMapper        the session record mapper
     * @param attendanceOutcomeService   the attendance outcome service (INDIVIDUAL sessions)
     * @param groupSessionRecordService  the group session record service
     * @param groupAttendanceService     the group attendance service
     */
    public SessionRecordController(
            final SessionRecordService sessionRecordService,
            final SessionRecordMapper sessionRecordMapper,
            final AttendanceOutcomeService attendanceOutcomeService,
            final GroupSessionRecordService groupSessionRecordService,
            final GroupAttendanceService groupAttendanceService) {
        this.sessionRecordService = sessionRecordService;
        this.sessionRecordMapper = sessionRecordMapper;
        this.attendanceOutcomeService = attendanceOutcomeService;
        this.groupSessionRecordService = groupSessionRecordService;
        this.groupAttendanceService = groupAttendanceService;
    }

    /**
     * Queries session records with optional filters.
     *
     * <p>GET /api/sessions?therapistId={uuid}&startDate={date}&endDate={date}&status={status}
     *
     * <p>Returns sessions ordered by session date descending. All query parameters are optional.
     *
     * @param therapistId optional therapist UUID filter
     * @param startDate optional start date filter (ISO format: yyyy-MM-dd)
     * @param endDate optional end date filter (ISO format: yyyy-MM-dd)
     * @param status optional status filter (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
     * @return 200 OK with list of session records (may be empty)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<SessionRecordResponse>> getSessions(
            @RequestParam(required = false) final UUID therapistId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate endDate,
            @RequestParam(required = false) final SessionStatus status) {

        LOG.debug("GET /api/sessions: therapistId={}, startDate={}, endDate={}, status={}",
                therapistId, startDate, endDate, status);

        final List<SessionRecord> sessions = sessionRecordService.getSessions(
                therapistId, startDate, endDate, status);

        final List<SessionRecordResponse> responses = sessions.stream()
                .map(sessionRecordMapper::toResponse)
                .toList();

        LOG.info("Returning {} session records", responses.size());

        return ResponseEntity.ok(responses);
    }

    /**
     * Manually starts a session from an appointment.
     *
     * <p>POST /api/sessions/start
     *
     * <p>Creates a session record with status IN_PROGRESS and updates the appointment
     * status to IN_PROGRESS if it was previously SCHEDULED.
     *
     * @param request start session request with appointment ID
     * @param principal authenticated principal
     * @return 201 Created with session record, 409 Conflict if session already exists
     */
    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SessionRecordResponse> startSession(
            @Valid @RequestBody final StartSessionRequest request,
            final Principal principal) {

        final String principalName = principal.getName();

        // TODO: Extract actual user UUID from JWT - for now using mock UUID
        final UUID actorUserId = UUID.randomUUID();

        try {
            final SessionRecord session = sessionRecordService.startSession(
                    request.appointmentId(),
                    actorUserId,
                    principalName
            );

            final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

            LOG.info("Session started via API: sessionId={}, appointmentId={}, actor={}",
                    session.getId(), request.appointmentId(), principalName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            // Session already exists - return 409 Conflict
            LOG.warn("Duplicate session start attempt: appointmentId={}, actor={}",
                    request.appointmentId(), principalName);
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    /**
     * Retrieves a session record by its ID.
     *
     * <p>GET /api/sessions/{sessionId}
     *
     * @param sessionId session UUID
     * @return 200 OK with session record, 404 Not Found if session does not exist
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SessionRecordResponse> getSessionRecord(@PathVariable final UUID sessionId) {

        final SessionRecord session = sessionRecordService.getSessionRecord(sessionId);
        final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a session record by appointment ID.
     *
     * <p>GET /api/sessions/by-appointment/{appointmentId}
     *
     * @param appointmentId appointment UUID
     * @return 200 OK with session record, 404 Not Found if no session found for appointment
     */
    @GetMapping("/by-appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SessionRecordResponse> getSessionByAppointment(
            @PathVariable final UUID appointmentId) {

        final SessionRecord session = sessionRecordService.getSessionByAppointmentId(appointmentId);
        final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

        return ResponseEntity.ok(response);
    }

    /**
     * Completes an in-progress session with clinical notes and optional actual end time.
     *
     * <p>POST /api/sessions/{sessionId}/complete
     *
     * @param sessionId session UUID
     * @param request completion request with notes and optional end time
     * @param principal authenticated principal
     * @return 200 OK with updated session record
     * @throws ResponseStatusException 400 Bad Request if session is not IN_PROGRESS,
     *                                 404 Not Found if session does not exist
     */
    @PostMapping("/{sessionId}/complete")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SessionRecordResponse> completeSession(
            @PathVariable final UUID sessionId,
            @Valid @RequestBody final CompleteSessionRequest request,
            final Principal principal) {

        final String principalName = principal.getName();

        try {
            final SessionRecord session = sessionRecordService.completeSession(sessionId, request);
            final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

            LOG.info("Session completed via API: sessionId={}, actor={}", sessionId, principalName);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            LOG.warn("Invalid session completion attempt: sessionId={}, actor={}, error={}",
                    sessionId, principalName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Cancels a pending or in-progress session with a required reason.
     *
     * <p>POST /api/sessions/{sessionId}/cancel
     *
     * @param sessionId session UUID
     * @param request cancellation request with reason
     * @param principal authenticated principal
     * @return 200 OK with updated session record
     * @throws ResponseStatusException 400 Bad Request if session is not PENDING or IN_PROGRESS,
     *                                 404 Not Found if session does not exist
     */
    @PostMapping("/{sessionId}/cancel")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SessionRecordResponse> cancelSession(
            @PathVariable final UUID sessionId,
            @Valid @RequestBody final CancelSessionRequest request,
            final Principal principal) {

        final String principalName = principal.getName();

        try {
            final SessionRecord session = sessionRecordService.cancelSession(sessionId, request);
            final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

            LOG.info("Session cancelled via API: sessionId={}, actor={}, reason={}",
                    sessionId, principalName, request.reason());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            LOG.warn("Invalid session cancellation attempt: sessionId={}, actor={}, error={}",
                    sessionId, principalName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Records an attendance outcome for a session.
     *
     * <p>PATCH /api/sessions/{sessionId}/attendance
     *
     * <p>If the requested outcome is {@code LATE_CANCELLATION} but the {@code cancelledAt}
     * timestamp is outside the configured late-cancellation window, the effective outcome is
     * downgraded to {@code CANCELLED}. The response indicates both requested and effective outcomes.
     *
     * @param sessionId session UUID
     * @param request   attendance outcome request
     * @return 200 OK with attendance outcome response
     * @throws ResponseStatusException 404 Not Found if session does not exist
     */
    @PatchMapping("/{sessionId}/attendance")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AttendanceOutcomeResponse> recordAttendanceOutcome(
            @PathVariable final UUID sessionId,
            @Valid @RequestBody final RecordAttendanceRequest request) {

        final UUID actorUserId = getCurrentUserId();

        final AttendanceOutcomeResponse response =
                attendanceOutcomeService.recordOutcome(sessionId, request, actorUserId);

        LOG.info("Attendance outcome recorded via API: sessionId={}, outcome={}, actor={}",
                sessionId, response.effectiveOutcome(), actorUserId);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Group session endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a GROUP session record for the given appointment with the supplied participant list.
     *
     * <p>POST /api/sessions/group
     *
     * <p>Request body: {@code {"appointmentId": "<uuid>", "clientIds": ["<uuid>", ...], "status": "PENDING"}}
     *
     * @param request   map with appointmentId, clientIds list, and optional status
     * @param principal authenticated principal
     * @return 201 Created with session record
     */
    @PostMapping("/group")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SessionRecordResponse> createGroupSession(
            @Valid @RequestBody final Map<String, Object> request,
            final Principal principal) {

        final UUID appointmentId = UUID.fromString((String) request.get("appointmentId"));
        @SuppressWarnings("unchecked")
        final List<String> rawIds = (List<String>) request.get("clientIds");
        final List<UUID> clientIds = rawIds.stream().map(UUID::fromString).toList();

        final String statusStr = (String) request.getOrDefault("status", "PENDING");
        final SessionStatus status = SessionStatus.valueOf(statusStr);

        final UUID actorUserId = getCurrentUserId();

        try {
            final SessionRecord session = groupSessionRecordService.createGroupSession(
                    appointmentId, clientIds, status, actorUserId, principal.getName());

            final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

            LOG.info("GROUP session created via API: sessionId={}, appointmentId={}, participants={}, actor={}",
                    session.getId(), appointmentId, clientIds.size(), principal.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Returns the active participant list for a GROUP session.
     *
     * <p>GET /api/sessions/{sessionId}/participants
     *
     * @param sessionId session record UUID
     * @return 200 OK with participant list
     */
    @GetMapping("/{sessionId}/participants")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<GroupSessionParticipantResponse>> getGroupParticipants(
            @PathVariable final UUID sessionId) {

        final List<SessionParticipant> participants =
                groupSessionRecordService.getActiveParticipants(sessionId);

        // Delegate full enrichment (client names + attendance) to mapper
        final SessionRecord session = sessionRecordService.getSessionRecord(sessionId);
        final SessionRecordResponse full = sessionRecordMapper.toResponse(session);

        return ResponseEntity.ok(full.participants());
    }

    /**
     * Records the attendance outcome for a single client within a GROUP session.
     *
     * <p>PATCH /api/sessions/{sessionId}/participants/{clientId}/attendance
     *
     * @param sessionId   session record UUID
     * @param clientId    client UUID
     * @param request     attendance outcome request
     * @return 200 OK with per-client attendance record summary
     */
    @PatchMapping("/{sessionId}/participants/{clientId}/attendance")
    @PreAuthorize("hasAnyRole('THERAPIST', 'RECEPTION_ADMIN_STAFF', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AttendanceOutcomeResponse> recordGroupAttendance(
            @PathVariable final UUID sessionId,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final RecordGroupAttendanceRequest request) {

        final UUID actorUserId = getCurrentUserId();

        try {
            final var saved = groupAttendanceService.recordOutcomeForClient(
                    sessionId, clientId, request, actorUserId);

            final AttendanceOutcomeResponse response = new AttendanceOutcomeResponse(
                    sessionId,
                    saved.getAttendanceOutcome(),
                    request.outcome(),
                    request.cancelledAt(),
                    saved.getUpdatedAt());

            LOG.info("Group attendance recorded via API: sessionId={}, clientId={}, outcome={}, actor={}",
                    sessionId, clientId, saved.getAttendanceOutcome(), actorUserId);

            return ResponseEntity.ok(response);

        } catch (jakarta.persistence.EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private UUID getCurrentUserId() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                return UUID.fromString(auth.getName());
            } catch (final IllegalArgumentException e) {
                LOG.warn("Failed to parse user ID from authentication: {}", auth.getName());
                return UUID.randomUUID();
            }
        }
        return UUID.randomUUID();
    }
}
