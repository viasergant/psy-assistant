package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.domain.TherapistLeave;
import com.psyassistant.scheduling.dto.LeaveApprovalRequest;
import com.psyassistant.scheduling.service.TherapistLeaveService;
import com.psyassistant.users.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for administrative leave request management.
 *
 * <p>Admin-level operations for reviewing and managing leave requests across all therapists.
 *
 * <p>Role-based access control:
 * <ul>
 *     <li>SYSTEM_ADMINISTRATOR: full access to all operations</li>
 *     <li>RECEPTION_ADMIN_STAFF: full access to all operations</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/leave")
public class LeaveAdminController {

    private final TherapistLeaveService leaveService;

    /**
     * Constructs the admin leave controller.
     *
     * @param leaveService leave service
     */
    public LeaveAdminController(final TherapistLeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Retrieves all pending leave requests across all therapists.
     *
     * <p>Returns pending requests ordered by submission date (oldest first).
     *
     * @return list of pending leave requests
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")
    public ResponseEntity<List<TherapistLeave>> getAllPendingLeaveRequests() {
        final List<TherapistLeave> pending = leaveService.getAllPendingLeaveRequests();
        return ResponseEntity.ok(pending);
    }

    /**
     * Approves a leave request.
     *
     * <p>Updates the leave status to APPROVED and records the reviewer and admin notes.
     *
     * @param leaveId leave request UUID
     * @param request approval details including reviewer user ID and optional notes
     * @return updated leave request
     */
    @PutMapping("/{leaveId}/approve")
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
     * Rejects a leave request.
     *
     * <p>Updates the leave status to REJECTED and records the reviewer and admin notes.
     *
     * @param leaveId leave request UUID
     * @param request rejection details including reviewer user ID and optional notes
     * @return updated leave request
     */
    @PutMapping("/{leaveId}/reject")
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
}
