package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.TherapistRecurringSchedule;
import com.psyassistant.scheduling.domain.TherapistScheduleOverride;
import com.psyassistant.scheduling.dto.RecurringScheduleRequest;
import com.psyassistant.scheduling.dto.ScheduleOverrideRequest;
import com.psyassistant.scheduling.dto.ScheduleSummaryResponse;
import com.psyassistant.scheduling.service.TherapistLeaveService;
import com.psyassistant.scheduling.service.TherapistScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for therapist schedule management (recurring and overrides).
 *
 * <p>Role-based access control:
 * <ul>
 *     <li>SYSTEM_ADMINISTRATOR: full CRUD on all schedules</li>
 *     <li>RECEPTION_ADMIN_STAFF + THERAPIST: CRUDon own schedule</li>
 *     <li>THERAPIST only: read-only</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/schedules/therapists/{therapistProfileId}")
public class TherapistScheduleController {

    private final TherapistScheduleService scheduleService;
    private final TherapistLeaveService leaveService;

    /**
     * Constructs the schedule controller.
     *
     * @param scheduleService schedule service
     * @param leaveService leave service
     */
    public TherapistScheduleController(final TherapistScheduleService scheduleService,
                                        final TherapistLeaveService leaveService) {
        this.scheduleService = scheduleService;
        this.leaveService = leaveService;
    }

    // ========== Schedule Summary ==========

    /**
     * Retrieves complete schedule summary (recurring + overrides + leave).
     *
     * @param therapistProfileId therapist profile UUID
     * @return schedule summary
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<ScheduleSummaryResponse> getScheduleSummary(
        @PathVariable final UUID therapistProfileId,
        @RequestParam(required = false) final LocalDate startDate,
        @RequestParam(required = false) final LocalDate endDate
    ) {
        // TODO: Add access control check - therapist can only view own schedule unless admin

        final List<TherapistRecurringSchedule> recurring = scheduleService.getRecurringSchedule(therapistProfileId);

        final LocalDate start = startDate != null ? startDate : LocalDate.now();
        final LocalDate end = endDate != null ? endDate : start.plusMonths(1);

        final List<TherapistScheduleOverride> overrides =
            scheduleService.getOverridesInRange(therapistProfileId, start, end);

        final var leave = leaveService.getAllLeave(therapistProfileId);

        final var response = new ScheduleSummaryResponse(
            therapistProfileId,
            recurring.stream()
                .map(r -> new ScheduleSummaryResponse.RecurringScheduleEntry(
                    r.getId(),
                    r.getDayOfWeek(),
                    r.getStartTime(),
                    r.getEndTime(),
                    r.getTimezone()
                ))
                .collect(Collectors.toList()),
            overrides.stream()
                .map(o -> new ScheduleSummaryResponse.OverrideEntry(
                    o.getId(),
                    o.getOverrideDate(),
                    o.getIsAvailable(),
                    o.getStartTime(),
                    o.getEndTime(),
                    o.getReason()
                ))
                .collect(Collectors.toList()),
            leave.stream()
                .map(l -> new ScheduleSummaryResponse.LeaveEntry(
                    l.getId(),
                    l.getStartDate(),
                    l.getEndDate(),
                    l.getLeaveType().toString(),
                    l.getStatus().toString(),
                    l.getRequestNotes(),
                    l.getAdminNotes()
                ))
                .collect(Collectors.toList())
        );

        return ResponseEntity.ok(response);
    }

    // ========== Recurring Schedule CRUD ==========

    /**
     * Creates a new recurring schedule entry.
     *
     * @param therapistProfileId therapist profile UUID
     * @param request recurring schedule details
     * @return created schedule entry
     */
    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<TherapistRecurringSchedule> createRecurringSchedule(
        @PathVariable final UUID therapistProfileId,
        @Valid @RequestBody final RecurringScheduleRequest request
    ) {
        // TODO: Add access control check - reception staff can only edit own schedule

        final var created = scheduleService.createRecurringSchedule(
            therapistProfileId,
            request.dayOfWeek(),
            request.startTime(),
            request.endTime(),
            request.timezone()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing recurring schedule entry.
     *
     * @param therapistProfileId therapist profile UUID
     * @param scheduleId schedule entry UUID
     * @param request updated schedule details
     * @return updated schedule entry
     */
    @PutMapping("/recurring/{scheduleId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<TherapistRecurringSchedule> updateRecurringSchedule(
        @PathVariable final UUID therapistProfileId,
        @PathVariable final UUID scheduleId,
        @Valid @RequestBody final RecurringScheduleRequest request
    ) {
        // TODO: Add access control check

        final var updated = scheduleService.updateRecurringSchedule(
            scheduleId,
            request.startTime(),
            request.endTime(),
            request.timezone()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a recurring schedule entry.
     *
     * @param therapistProfileId therapist profile UUID
     * @param scheduleId schedule entry UUID
     * @return no content
     */
    @DeleteMapping("/recurring/{scheduleId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<Void> deleteRecurringSchedule(
        @PathVariable final UUID therapistProfileId,
        @PathVariable final UUID scheduleId
    ) {
        // TODO: Add access control check

        scheduleService.deleteRecurringSchedule(scheduleId);

        return ResponseEntity.noContent().build();
    }

    // ========== Schedule Override CRUD ==========

    /**
     * Creates a schedule override.
     *
     * @param therapistProfileId therapist profile UUID
     * @param request override details
     * @return created override
     */
    @PostMapping("/overrides")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<TherapistScheduleOverride> createOverride(
        @PathVariable final UUID therapistProfileId,
        @Valid @RequestBody final ScheduleOverrideRequest request
    ) {
        // TODO: Add access control check

        final TherapistScheduleOverride created;

        if (request.isAvailable()) {
            if (request.startTime() == null || request.endTime() == null) {
                throw new IllegalArgumentException("Start and end times are required when available");
            }
            created = scheduleService.createCustomHoursOverride(
                therapistProfileId,
                request.overrideDate(),
                request.startTime(),
                request.endTime(),
                request.reason()
            );
        } else {
            created = scheduleService.createUnavailableOverride(
                therapistProfileId,
                request.overrideDate(),
                request.reason()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates a schedule override.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideId override UUID
     * @param request updated override details
     * @return updated override
     */
    @PutMapping("/overrides/{overrideId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<TherapistScheduleOverride> updateOverride(
        @PathVariable final UUID therapistProfileId,
        @PathVariable final UUID overrideId,
        @Valid @RequestBody final ScheduleOverrideRequest request
    ) {
        // TODO: Add access control check

        final var updated = scheduleService.updateOverride(
            overrideId,
            request.isAvailable(),
            request.startTime(),
            request.endTime(),
            request.reason()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a schedule override.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideId override UUID
     * @return no content
     */
    @DeleteMapping("/overrides/{overrideId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<Void> deleteOverride(
        @PathVariable final UUID therapistProfileId,
        @PathVariable final UUID overrideId
    ) {
        // TODO: Add access control check

        scheduleService.deleteOverride(overrideId);

        return ResponseEntity.noContent().build();
    }
}
