package com.psyassistant.sessions.rest;

import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.dto.SessionRecordMapper;
import com.psyassistant.sessions.dto.SessionRecordResponse;
import com.psyassistant.sessions.dto.StartSessionRequest;
import com.psyassistant.sessions.service.SessionRecordService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
 * <p>Authorization: Requires authenticated therapist, admin, or receptionist roles.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionRecordController {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRecordController.class);

    private final SessionRecordService sessionRecordService;
    private final SessionRecordMapper sessionRecordMapper;

    public SessionRecordController(final SessionRecordService sessionRecordService,
                                    final SessionRecordMapper sessionRecordMapper) {
        this.sessionRecordService = sessionRecordService;
        this.sessionRecordMapper = sessionRecordMapper;
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'ADMIN', 'RECEPTIONIST')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'ADMIN', 'RECEPTIONIST')")
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
    @PreAuthorize("hasAnyRole('THERAPIST', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<SessionRecordResponse> getSessionByAppointment(
            @PathVariable final UUID appointmentId) {

        final SessionRecord session = sessionRecordService.getSessionByAppointmentId(appointmentId);
        final SessionRecordResponse response = sessionRecordMapper.toResponse(session);

        return ResponseEntity.ok(response);
    }
}
