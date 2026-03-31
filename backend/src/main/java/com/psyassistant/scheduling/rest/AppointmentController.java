package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.dto.AppointmentMapper;
import com.psyassistant.scheduling.dto.AppointmentResponse;
import com.psyassistant.scheduling.dto.CancelAppointmentRequest;
import com.psyassistant.scheduling.dto.CheckConflictsRequest;
import com.psyassistant.scheduling.dto.ConflictCheckResponse;
import com.psyassistant.scheduling.dto.CreateAppointmentRequest;
import com.psyassistant.scheduling.dto.RescheduleAppointmentRequest;
import com.psyassistant.scheduling.dto.SessionTypeResponse;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import com.psyassistant.scheduling.service.AppointmentService;
import com.psyassistant.scheduling.service.ConflictDetectionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for appointment booking operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *     <li>Creating appointments with conflict detection</li>
 *     <li>Pre-flight conflict checking</li>
 * </ul>
 *
 * <p><strong>Security</strong>: All endpoints require STAFF role or higher.
 */
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;
    private final ConflictDetectionService conflictDetectionService;
    private final AppointmentMapper appointmentMapper;
    private final SessionTypeRepository sessionTypeRepository;

    public AppointmentController(final AppointmentService appointmentService,
                                  final ConflictDetectionService conflictDetectionService,
                                  final AppointmentMapper appointmentMapper,
                                  final SessionTypeRepository sessionTypeRepository) {
        this.appointmentService = appointmentService;
        this.conflictDetectionService = conflictDetectionService;
        this.appointmentMapper = appointmentMapper;
        this.sessionTypeRepository = sessionTypeRepository;
    }

    /**
     * Retrieves all active session types for dropdown population.
     *
     * <p>Returns only active session types ordered alphabetically by name.
     * Used for populating session type dropdown in appointment booking UI.
     *
     * @return list of active session types (200 OK)
     */
    @GetMapping("/session-types")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<SessionTypeResponse>> getSessionTypes() {
        LOG.debug("Fetching active session types");

        final List<SessionTypeResponse> sessionTypes = sessionTypeRepository.findByIsActiveTrueOrderByName()
                .stream()
                .map(st -> new SessionTypeResponse(
                        st.getId(),
                        st.getCode(),
                        st.getName(),
                        st.getDescription()
                ))
                .toList();

        LOG.debug("Found {} active session types", sessionTypes.size());
        return ResponseEntity.ok(sessionTypes);
    }

    /**
     * Creates a new appointment.
     *
     * <p><strong>Conflict Handling</strong>:
     * <ul>
     *     <li>If {@code allowConflictOverride = false} and conflicts exist: returns 409 Conflict</li>
     *     <li>If {@code allowConflictOverride = true}: creates appointment with override flag</li>
     *     <li>If no conflicts: creates appointment normally</li>
     * </ul>
     *
     * <p><strong>Required Permission</strong>: {@code STAFF} role or higher.
     * Conflict override additionally requires {@code OVERRIDE_BOOKING} permission (future PA-32).
     *
     * @param request appointment creation request
     * @return created appointment (201 Created)
     * @throws AppointmentService.ConflictException if conflicts exist and override not allowed (409 Conflict)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody final CreateAppointmentRequest request) {

        final UUID actorUserId = getCurrentUserId();
        final String actorName = getCurrentUsername();

        LOG.info("Creating appointment: therapist={}, client={}, startTime={}, duration={}, allowOverride={}",
                request.therapistProfileId(),
                request.clientId(),
                request.startTime(),
                request.durationMinutes(),
                request.allowConflictOverride());

        final Appointment appointment = appointmentService.createAppointment(
                request.therapistProfileId(),
                request.clientId(),
                request.sessionTypeId(),
                request.startTime(),
                request.durationMinutes(),
                request.timezone(),
                request.notes(),
                request.allowConflictOverride(),
                actorUserId,
                actorName
        );

        final AppointmentResponse response = appointmentMapper.toResponse(appointment);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Checks for appointment conflicts without creating an appointment.
     *
     * <p>Pre-flight check to show potential conflicts before the user submits the form.
     * Returns list of conflicting appointments with minimal details for UI display.
     *
     * @param request conflict check request
     * @return conflict check result with list of conflicting appointments
     */
    @PostMapping("/check-conflicts")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ConflictCheckResponse> checkConflicts(
            @Valid @RequestBody final CheckConflictsRequest request) {

        LOG.debug("Checking conflicts: therapist={}, startTime={}, duration={}",
                request.therapistProfileId(),
                request.startTime(),
                request.durationMinutes());

        final List<Appointment> conflicts = conflictDetectionService.findConflictingAppointments(
                request.therapistProfileId(),
                request.startTime(),
                request.durationMinutes()
        );

        final List<ConflictCheckResponse.ConflictingAppointment> conflictDtos = conflicts.stream()
                .map(appointmentMapper::toConflictDto)
                .toList();

        final ConflictCheckResponse response = new ConflictCheckResponse(
                !conflicts.isEmpty(),
                conflictDtos
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves an appointment by ID.
     *
     * @param appointmentId appointment UUID
     * @return appointment details
     * @throws EntityNotFoundException if not found (404 Not Found)
     */
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @PathVariable final UUID appointmentId) {

        LOG.debug("Fetching appointment: id={}", appointmentId);

        final Appointment appointment = appointmentService.getAppointment(appointmentId);
        final AppointmentResponse response = appointmentMapper.toResponse(appointment);

        return ResponseEntity.ok(response);
    }

    /**
     * Reschedules an existing appointment to a new time.
     *
     * <p>Applies conflict detection to the new time slot (excluding this appointment).
     * Preserves original start time and tracks reschedule reason.
     *
     * @param appointmentId appointment UUID to reschedule
     * @param request reschedule request with new time and reason
     * @return rescheduled appointment (200 OK)
     * @throws EntityNotFoundException if appointment not found (404)
     * @throws IllegalStateException if appointment already cancelled (400)
     * @throws AppointmentService.ConflictException if new time conflicts and override not allowed (409)
     */
    @PutMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable final UUID appointmentId,
            @Valid @RequestBody final RescheduleAppointmentRequest request) {

        final UUID actorUserId = getCurrentUserId();
        final String actorName = getCurrentUsername();

        LOG.info("Rescheduling appointment: id={}, newStartTime={}, reason={}",
                appointmentId, request.newStartTime(), request.reason());

        final Appointment rescheduled = appointmentService.rescheduleAppointment(
                appointmentId,
                request.newStartTime(),
                request.reason(),
                request.allowConflictOverride(),
                actorUserId,
                actorName
        );

        final AppointmentResponse response = appointmentMapper.toResponse(rescheduled);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an existing appointment.
     *
     * <p>Once cancelled, the time slot becomes available for new bookings.
     * Records cancellation type, reason, timestamp, and cancelling user.
     *
     * @param appointmentId appointment UUID to cancel
     * @param request cancellation request with type and reason
     * @return cancelled appointment (200 OK)
     * @throws EntityNotFoundException if appointment not found (404)
     * @throws IllegalStateException if appointment already cancelled (400)
     */
    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable final UUID appointmentId,
            @Valid @RequestBody final CancelAppointmentRequest request) {

        final UUID actorUserId = getCurrentUserId();
        final String actorName = getCurrentUsername();

        LOG.info("Cancelling appointment: id={}, type={}, reason={}",
                appointmentId, request.cancellationType(), request.reason());

        final Appointment cancelled = appointmentService.cancelAppointment(
                appointmentId,
                request.cancellationType(),
                request.reason(),
                actorUserId,
                actorName
        );

        final AppointmentResponse response = appointmentMapper.toResponse(cancelled);

        return ResponseEntity.ok(response);
    }

    // ========== Exception Handlers ==========

    /**
     * Handles appointment conflicts when override is not allowed.
     *
     * @param ex conflict exception with conflicting appointments
     * @return 409 Conflict with conflict details
     */
    @ExceptionHandler(AppointmentService.ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictException(
            final AppointmentService.ConflictException ex) {

        LOG.warn("Appointment conflict: {}", ex.getMessage());

        final List<ConflictCheckResponse.ConflictingAppointment> conflictDtos =
                ex.getConflictingAppointments().stream()
                        .map(appointmentMapper::toConflictDto)
                        .toList();

        final Map<String, Object> errorResponse = Map.of(
                "error", "APPOINTMENT_CONFLICT",
                "message", "Appointment conflicts with existing bookings",
                "conflicts", conflictDtos
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles validation errors (400 Bad Request).
     *
     * @param ex illegal argument exception from validation
     * @return 400 Bad Request with error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            final IllegalArgumentException ex) {

        LOG.warn("Validation error: {}", ex.getMessage());

        final Map<String, String> errorResponse = Map.of(
                "error", "VALIDATION_ERROR",
                "message", ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles entity not found errors (404 Not Found).
     *
     * @param ex entity not found exception
     * @return 404 Not Found with error message
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(
            final EntityNotFoundException ex) {

        LOG.warn("Entity not found: {}", ex.getMessage());

        final Map<String, String> errorResponse = Map.of(
                "error", "NOT_FOUND",
                "message", ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles illegal state errors (400 Bad Request).
     *
     * @param ex illegal state exception (e.g., rescheduling cancelled appointment)
     * @return 400 Bad Request with error message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(
            final IllegalStateException ex) {

        LOG.warn("Illegal state: {}", ex.getMessage());

        final Map<String, String> errorResponse = Map.of(
                "error", "ILLEGAL_STATE",
                "message", ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== Security Helpers ==========

    /**
     * Gets the current authenticated user's UUID.
     *
     * @return user UUID, or a system UUID if not authenticated
     */
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

    /**
     * Gets the current authenticated user's display name.
     *
     * @return username or "System" if not authenticated
     */
    private String getCurrentUsername() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return "System";
    }
}
