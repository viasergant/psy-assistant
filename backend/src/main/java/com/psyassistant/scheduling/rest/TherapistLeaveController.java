package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.LeaveStatus;
import com.psyassistant.scheduling.domain.TherapistLeave;
import com.psyassistant.scheduling.dto.ConflictWarningResponse;
import com.psyassistant.scheduling.dto.LeaveApprovalRequest;
import com.psyassistant.scheduling.dto.LeaveRequestSubmission;
import com.psyassistant.scheduling.service.TherapistLeaveService;
import com.psyassistant.users.UserManagementService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * <p>Therapist-specific leave operations. For admin operations (viewing all pending requests,
 * approving/rejecting), see {@link LeaveAdminController}.
 *
 * <p>Role-based access control:
 * <ul>
 *     <li>SYSTEM_ADMINISTRATOR: full access - submit, cancel, view all</li>
 *     <li>RECEPTION_ADMIN_STAFF: submit, cancel, view (approve/reject via admin endpoints)</li>
 *     <li>THERAPIST: submit, cancel, view own</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/therapists/{therapistProfileId}")
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
     * Resolves "me" token to the actual therapist profile ID from JWT.
     *
     * @param pathParam the path parameter (either "me" or a UUID string)
     * @return the resolved UUID
     * @throws IllegalArgumentException if pathParam is "me" but no therapistProfileId in JWT
     */
    private UUID resolveTherapistProfileId(final String pathParam) {
        if ("me".equals(pathParam)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String therapistProfileId = jwt.getClaimAsString("therapistProfileId");
                if (therapistProfileId != null) {
                    return UUID.fromString(therapistProfileId);
                }
            }
            throw new IllegalArgumentException(
                "Cannot resolve 'me': therapistProfileId not found in JWT token");
        }
        return UUID.fromString(pathParam);
    }

    /**
     * Submits a new leave request.
     *
     * @param therapistProfileIdParam therapist profile UUID or "me"
     * @param request leave request details
     * @return created leave request
     */
    @PostMapping("/leave")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<TherapistLeave> submitLeaveRequest(
        @PathVariable("therapistProfileId") final String therapistProfileIdParam,
        @Valid @RequestBody final LeaveRequestSubmission request
    ) {
        final UUID therapistProfileId = resolveTherapistProfileId(therapistProfileIdParam);
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
     * <p>Note: For better API design, consider using {@code POST /api/v1/admin/leave/{leaveId}/approve}
     * instead (see {@link LeaveAdminController}).
     *
     * @param leaveId leave request UUID
     * @param request approval details
     * @return updated leave request
     */
    @PutMapping("/leave/{leaveId}/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<TherapistLeave> approveLeaveRequest(
        @PathVariable final UUID leaveId,
        @Valid @RequestBody final LeaveApprovalRequest request
    ) {
        final UUID reviewerId = UserManagementService.currentPrincipalId();
        if (reviewerId == null) {
            throw new IllegalStateException("Unable to determine reviewer from authentication context");
        }

        final var approved = leaveService.approveLeaveRequest(
            leaveId,
            reviewerId,
            request.adminNotes()
        );

        return ResponseEntity.ok(approved);
    }

    /**
     * Rejects a leave request (admin).
     *
     * <p>Note: For better API design, consider using {@code PUT /api/v1/admin/leave/{leaveId}/reject}
     * instead (see {@link LeaveAdminController}).
     *
     * @param leaveId leave request UUID
     * @param request rejection details
     * @return updated leave request
     */
    @PutMapping("/leave/{leaveId}/reject")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<TherapistLeave> rejectLeaveRequest(
        @PathVariable final UUID leaveId,
        @Valid @RequestBody final LeaveApprovalRequest request
    ) {
        final UUID reviewerId = UserManagementService.currentPrincipalId();
        if (reviewerId == null) {
            throw new IllegalStateException("Unable to determine reviewer from authentication context");
        }

        final var rejected = leaveService.rejectLeaveRequest(
            leaveId,
            reviewerId,
            request.adminNotes()
        );

        return ResponseEntity.ok(rejected);
    }

    /**
     * Cancels a leave request.
     *
     * @param leaveId leave request UUID
     * @return updated leave request
     */
    @PutMapping("/leave/{leaveId}/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<TherapistLeave> cancelLeaveRequest(@PathVariable final UUID leaveId) {
        // TODO: Add access control check - therapist can only cancel their own requests

        final var cancelled = leaveService.cancelLeaveRequest(leaveId);

        return ResponseEntity.ok(cancelled);
    }

    /**
     * Retrieves all leave periods for a therapist.
     *
     * @param therapistProfileId therapist profile UUID
     * @param status optional status filter
     * @return list of leave periods
     */
    @GetMapping("/leave")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<List<TherapistLeave>> getLeaveForTherapist(
        @PathVariable("therapistProfileId") final String therapistProfileIdParam,
        @RequestParam(required = false) final LeaveStatus status
    ) {
        final UUID therapistProfileId = resolveTherapistProfileId(therapistProfileIdParam);
        // TODO: Add access control check - therapist can only view own leave

        final List<TherapistLeave> leave = status != null
            ? leaveService.getLeaveByStatus(therapistProfileId, status)
            : leaveService.getAllLeave(therapistProfileId);

        return ResponseEntity.ok(leave);
    }

    /**
     * Retrieves all pending leave requests (admin view).
     *
     * <p>Note: This endpoint is incorrectly placed under the therapist-specific path.
     * Consider using {@code GET /api/v1/admin/leave/pending} instead (see {@link LeaveAdminController}).
     *
     * @return list of pending leave requests
     */
    @GetMapping("/leave/pending")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
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
    @GetMapping("/leave/conflicts")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<ConflictWarningResponse> checkLeaveConflicts(
        @PathVariable("therapistProfileId") final String therapistProfileIdParam,
        @RequestParam final String startDate,
        @RequestParam final String endDate
    ) {
        final UUID therapistProfileId = resolveTherapistProfileId(therapistProfileIdParam);
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
