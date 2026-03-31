package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.dto.AppointmentMapper;
import com.psyassistant.scheduling.dto.AppointmentResponse;
import com.psyassistant.scheduling.dto.CheckConflictsRequest;
import com.psyassistant.scheduling.dto.ConflictCheckResponse;
import com.psyassistant.scheduling.dto.CreateAppointmentRequest;
import com.psyassistant.scheduling.service.AppointmentService;
import com.psyassistant.scheduling.service.ConflictDetectionService;
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
import org.springframework.web.bind.annotation.PostMapping;
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

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;
    private final ConflictDetectionService conflictDetectionService;
    private final AppointmentMapper appointmentMapper;

    public AppointmentController(final AppointmentService appointmentService,
                                  final ConflictDetectionService conflictDetectionService,
                                  final AppointmentMapper appointmentMapper) {
        this.appointmentService = appointmentService;
        this.conflictDetectionService = conflictDetectionService;
        this.appointmentMapper = appointmentMapper;
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

        log.info("Creating appointment: therapist={}, client={}, startTime={}, duration={}, allowOverride={}",
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

        log.debug("Checking conflicts: therapist={}, startTime={}, duration={}",
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

        log.warn("Appointment conflict: {}", ex.getMessage());

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

        log.warn("Validation error: {}", ex.getMessage());

        final Map<String, String> errorResponse = Map.of(
                "error", "VALIDATION_ERROR",
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
                log.warn("Failed to parse user ID from authentication: {}", auth.getName());
                return UUID.randomUUID(); // Fallback for system operations
            }
        }
        return UUID.randomUUID(); // Fallback for non-authenticated operations
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
