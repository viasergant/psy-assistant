package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.domain.AppointmentSeries;
import com.psyassistant.scheduling.dto.AppointmentMapper;
import com.psyassistant.scheduling.dto.AppointmentResponse;
import com.psyassistant.scheduling.dto.AppointmentSeriesResponse;
import com.psyassistant.scheduling.dto.CancelRecurringOccurrenceRequest;
import com.psyassistant.scheduling.dto.CheckRecurringConflictsRequest;
import com.psyassistant.scheduling.dto.CreateRecurringSeriesRequest;
import com.psyassistant.scheduling.dto.CreateRecurringSeriesResponse;
import com.psyassistant.scheduling.dto.EditRecurringOccurrenceRequest;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse;
import com.psyassistant.scheduling.service.AppointmentService;
import com.psyassistant.scheduling.service.RecurringSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for recurring appointment series management.
 *
 * <p>All endpoints require STAFF role or higher — consistent with
 * {@link AppointmentController}.
 *
 * <p>Base path: {@code /api/v1/recurring-series}
 */
@RestController
@RequestMapping("/api/v1/recurring-series")
@Tag(name = "Recurring Series", description = "Manage recurring appointment series (PA-33)")
public class RecurringSeriesController {

    private static final Logger LOG = LoggerFactory.getLogger(RecurringSeriesController.class);

    private final RecurringSeriesService seriesService;
    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;

    public RecurringSeriesController(final RecurringSeriesService seriesService,
                                      final AppointmentService appointmentService,
                                      final AppointmentMapper appointmentMapper) {
        this.seriesService = seriesService;
        this.appointmentService = appointmentService;
        this.appointmentMapper = appointmentMapper;
    }

    // ========== Conflict Pre-Flight ==========

    /**
     * Checks every generated slot for conflicts without creating any records.
     *
     * @param request conflict check parameters
     * @return per-slot conflict results with aggregate counts (200 OK)
     */
    @PostMapping("/check-conflicts")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Pre-flight conflict check for a recurring series")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conflict check result"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public ResponseEntity<RecurringConflictCheckResponse> checkConflicts(
            @Valid @RequestBody final CheckRecurringConflictsRequest request) {

        LOG.debug("Recurring conflict check: therapist={}, startTime={}, occurrences={}",
                request.therapistProfileId(), request.startTime(), request.occurrences());

        final RecurringConflictCheckResponse response = seriesService.checkConflicts(
                request.therapistProfileId(),
                request.startTime(),
                request.durationMinutes(),
                request.timezone(),
                request.recurrenceType(),
                request.occurrences()
        );

        return ResponseEntity.ok(response);
    }

    // ========== Series Creation ==========

    /**
     * Creates a new recurring appointment series.
     *
     * @param request series creation parameters including conflict resolution
     * @return summary of saved and skipped occurrences (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Create a new recurring appointment series")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Series created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or ABORT with conflicts"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Conflicts detected and ABORT chosen")
    })
    public ResponseEntity<CreateRecurringSeriesResponse> createSeries(
            @Valid @RequestBody final CreateRecurringSeriesRequest request) {

        final UUID actorUserId = getCurrentUserId();
        final String actorName = getCurrentUsername();

        LOG.info("Creating recurring series: therapist={}, type={}, occurrences={}",
                request.therapistProfileId(), request.recurrenceType(), request.occurrences());

        final CreateRecurringSeriesResponse partial = seriesService.createSeries(request, actorUserId, actorName);

        // Fetch saved appointments to return full response
        final List<Appointment> saved = seriesService.getSeriesAppointments(partial.seriesId());
        final List<AppointmentResponse> appointmentResponses = saved.stream()
                .map(appointmentMapper::toResponse)
                .toList();

        final CreateRecurringSeriesResponse response = new CreateRecurringSeriesResponse(
                partial.seriesId(),
                partial.requestedOccurrences(),
                partial.savedOccurrences(),
                partial.skippedOccurrences(),
                appointmentResponses,
                partial.waitlistEntryIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== Get Series ==========

    /**
     * Retrieves a series with all its occurrences.
     *
     * @param seriesId series ID
     * @return series details plus all appointments (200 OK)
     */
    @GetMapping("/{seriesId}")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Get a recurring series with all occurrences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Series found"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Series not found")
    })
    public ResponseEntity<AppointmentSeriesResponse> getSeries(
            @PathVariable @Parameter(description = "Series ID") final Long seriesId) {

        LOG.debug("Fetching series: id={}", seriesId);

        final AppointmentSeries series = seriesService.getSeries(seriesId);
        final List<Appointment> appointments = seriesService.getSeriesAppointments(seriesId);
        final List<AppointmentResponse> appointmentResponses = appointments.stream()
                .map(appointmentMapper::toResponse)
                .toList();

        final AppointmentSeriesResponse response = new AppointmentSeriesResponse(
                series.getId(),
                series.getRecurrenceType(),
                series.getStartDate(),
                series.getTotalOccurrences(),
                series.getGeneratedOccurrences(),
                series.getTherapistProfileId(),
                series.getClientId(),
                new AppointmentResponse.SessionTypeInfo(
                        series.getSessionType().getId(),
                        series.getSessionType().getCode(),
                        series.getSessionType().getName(),
                        series.getSessionType().getDescription()
                ),
                series.getDurationMinutes(),
                series.getTimezone(),
                series.getStatus(),
                series.getCreatedAt(),
                series.getUpdatedAt(),
                series.getVersion(),
                appointmentResponses
        );

        return ResponseEntity.ok(response);
    }

    // ========== Edit Occurrence ==========

    /**
     * Edits a single occurrence or all future occurrences of a series.
     *
     * @param seriesId parent series ID
     * @param appointmentId the appointment being edited
     * @param request edit request
     * @return edited appointment (200 OK)
     */
    @PatchMapping("/{seriesId}/appointments/{appointmentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Edit a recurring occurrence (SINGLE or FUTURE_SERIES scope)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Occurrence(s) edited"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Series or appointment not found")
    })
    public ResponseEntity<AppointmentResponse> editOccurrence(
            @PathVariable final Long seriesId,
            @PathVariable final UUID appointmentId,
            @Valid @RequestBody final EditRecurringOccurrenceRequest request) {

        final UUID actorUserId = getCurrentUserId();
        final String actorName = getCurrentUsername();

        LOG.info("Editing occurrence: seriesId={}, appointmentId={}, scope={}",
                seriesId, appointmentId, request.editScope());

        final Appointment edited = seriesService.editOccurrence(
                seriesId, appointmentId, request, actorUserId, actorName);

        return ResponseEntity.ok(appointmentMapper.toResponse(edited));
    }

    // ========== Cancel Occurrence ==========

    /**
     * Cancels one or more occurrences (SINGLE / FUTURE_SERIES / ENTIRE_SERIES).
     *
     * @param seriesId parent series ID
     * @param appointmentId the appointment being cancelled (anchor)
     * @param request cancellation request with scope
     * @return 204 No Content on success
     */
    @DeleteMapping("/{seriesId}/appointments/{appointmentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Cancel occurrence(s) (SINGLE / FUTURE_SERIES / ENTIRE_SERIES scope)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Occurrence(s) cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Series or appointment not found")
    })
    public ResponseEntity<Void> cancelOccurrence(
            @PathVariable final Long seriesId,
            @PathVariable final UUID appointmentId,
            @Valid @RequestBody final CancelRecurringOccurrenceRequest request) {

        final UUID actorUserId = getCurrentUserId();
        final String actorName = getCurrentUsername();

        LOG.info("Cancelling occurrence(s): seriesId={}, appointmentId={}, scope={}",
                seriesId, appointmentId, request.cancelScope());

        seriesService.cancelOccurrences(
                seriesId, appointmentId, request, actorUserId, actorName, appointmentService);

        return ResponseEntity.noContent().build();
    }

    // ========== Exception Handlers ==========

    @ExceptionHandler(RecurringSeriesService.SeriesConflictException.class)
    public ResponseEntity<Map<String, Object>> handleSeriesConflict(
            final RecurringSeriesService.SeriesConflictException ex) {
        LOG.warn("Series conflict abort: {}", ex.getMessage());
        final Map<String, Object> body = Map.of(
                "error", "SERIES_CONFLICT",
                "message", ex.getMessage(),
                "conflictingSlots", ex.getConflictMap().keySet()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(final EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(final IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", "VALIDATION_ERROR", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(final IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", "ILLEGAL_STATE", "message", ex.getMessage()));
    }

    // ========== Security Helpers (copied from AppointmentController pattern) ==========

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

    private String getCurrentUsername() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return "System";
    }
}
