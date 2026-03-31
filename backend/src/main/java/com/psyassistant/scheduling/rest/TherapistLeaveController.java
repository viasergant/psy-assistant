package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.LeaveStatus;
import com.psyassistant.scheduling.domain.TherapistLeave;
import com.psyassistant.scheduling.dto.ConflictWarningResponse;
import com.psyassistant.scheduling.dto.LeaveApprovalRequest;
import com.psyassistant.scheduling.dto.LeaveRequestSubmission;
import com.psyassistant.scheduling.service.TherapistLeaveService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for therapist leave request management.
 *
 * <p>Role-based access control:
 * <ul>
 *     <li>SYSTEM_ADMINISTRATOR: approve, reject, view all</li>
 *     <li>THERAPIST: submit, cancel, view own</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/leave")
public class TherapistLeaveController {

    private final TherapistLeaveService leaveService;

    /**
     * Constructs the leave controller.
     *
     * @param leaveService leave service
     */
    public TherapistLeaveController(final TherapistLeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Submits a new leave request (therapist).
     *
     * @param therapistProfileId therapist profile UUID
     * @param request leave request details
     * @return created leave request
     */
    @PostMapping("/therapists/{therapistProfileId}/requests")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<TherapistLeave> submitLeaveRequest(
        @PathVariable final UUID therapistProfileId,
        @Valid @RequestBody final LeaveRequestSubmission request
    ) {
        // TODO: Add access control check - therapist can only submit for themselves

        final var created = leaveService.submitLeaveRequest(
            therapistProfileId,
            request.startDate(),
            request.endDate(),
            request.leaveType(),
            request.requestNotes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Approves a leave request (admin).
     *
     * @param leaveId leave request UUID
     * @param request approval details
     * @return updated leave request
     */
    @PutMapping("/requests/{leaveId}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<TherapistLeave> approveLeaveRequest(
        @PathVariable final UUID leaveId,
        @Valid @RequestBody final LeaveApprovalRequest request
    ) {
        final var approved = leaveService.approveLeaveRequest(
            leaveId,
            request.reviewerUserId(),
            request.adminNotes()
        );

        return ResponseEntity.ok(approved);
    }

    /**
     * Rejects a leave request (admin).
     *
     * @param leaveId leave request UUID
     * @param request rejection details
     * @return updated leave request
     */
    @PutMapping("/requests/{leaveId}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<TherapistLeave> rejectLeaveRequest(
        @PathVariable final UUID leaveId,
        @Valid @RequestBody final LeaveApprovalRequest request
    ) {
        final var rejected = leaveService.rejectLeaveRequest(
            leaveId,
            request.reviewerUserId(),
            request.adminNotes()
        );

        return ResponseEntity.ok(rejected);
    }

    /**
     * Cancels a leave request (therapist).
     *
     * @param leaveId leave request UUID
     * @return updated leave request
     */
    @PutMapping("/requests/{leaveId}/cancel")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<TherapistLeave> cancelLeaveRequest(@PathVariable final UUID leaveId) {
        // TODO: Add access control check - therapist can only cancel their own requests

        final var cancelled = leaveService.cancelLeaveRequest(leaveId);

        return ResponseEntity.ok(cancelled);
    }

    /**
     * Retrieves all leave periods for a therapist (therapist view).
     *
     * @param therapistProfileId therapist profile UUID
     * @param status optional status filter
     * @return list of leave periods
     */
    @GetMapping("/therapists/{therapistProfileId}/requests")
    @PreAuthorize("hasAnyRole('THERAPIST', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<TherapistLeave>> getLeaveForTherapist(
        @PathVariable final UUID therapistProfileId,
        @RequestParam(required = false) final LeaveStatus status
    ) {
        // TODO: Add access control check - therapist can only view own leave

        final List<TherapistLeave> leave = status != null
            ? leaveService.getLeaveByStatus(therapistProfileId, status)
            : leaveService.getAllLeave(therapistProfileId);

        return ResponseEntity.ok(leave);
    }

    /**
     * Retrieves all pending leave requests (admin view).
     *
     * @return list of pending leave requests
     */
    @GetMapping("/requests/pending")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<TherapistLeave>> getAllPendingLeaveRequests() {
        final List<TherapistLeave> pending = leaveService.getAllPendingLeaveRequests();

        return ResponseEntity.ok(pending);
    }

    /**
     * Checks for conflicts when submitting a leave request.
     *
     * <p>Returns existing appointments that overlap with the requested date range.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate start date
     * @param endDate end date
     * @return conflict warning response
     */
    @GetMapping("/therapists/{therapistProfileId}/conflicts")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ConflictWarningResponse> checkLeaveConflicts(
        @PathVariable final UUID therapistProfileId,
        @RequestParam final String startDate,
        @RequestParam final String endDate
    ) {
        // TODO: Query existing appointments and return conflicts
        // For now, return empty conflicts (appointments module not implemented yet)

        final var response = new ConflictWarningResponse(
            false,
            0,
            Collections.emptyList()
        );

        return ResponseEntity.ok(response);
    }
}
