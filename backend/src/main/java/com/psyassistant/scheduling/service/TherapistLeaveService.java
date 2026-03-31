package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.LeaveStatus;
import com.psyassistant.scheduling.domain.LeaveType;
import com.psyassistant.scheduling.domain.TherapistLeave;
import com.psyassistant.scheduling.repository.TherapistLeaveRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing therapist leave requests and approvals.
 *
 * <p>Handles the leave workflow:
 * <ol>
 *     <li>Therapist submits request → status = PENDING</li>
 *     <li>Admin reviews and approves/rejects</li>
 *     <li>Approved leave blocks dates in scheduling engine</li>
 *     <li>Therapist may cancel at any time</li>
 * </ol>
 *
 * <p>All state transitions are recorded in the audit trail.
 */
@Service
public class TherapistLeaveService {

    private final TherapistLeaveRepository leaveRepository;
    private final TherapistScheduleAuditService auditService;

    public TherapistLeaveService(final TherapistLeaveRepository leaveRepository,
                                  final TherapistScheduleAuditService auditService) {
        this.leaveRepository = leaveRepository;
        this.auditService = auditService;
    }

    /**
     * Submits a new leave request with PENDING status.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param leaveType type of leave
     * @param requestNotes optional therapist notes
     * @return created leave request
     * @throws IllegalArgumentException if date range is invalid or overlaps with existing active leave
     */
    @Transactional
    public TherapistLeave submitLeaveRequest(final UUID therapistProfileId,
                                              final LocalDate startDate,
                                              final LocalDate endDate,
                                              final LeaveType leaveType,
                                              final String requestNotes) {
        validateDateRange(startDate, endDate);
        validateNotPastDate(startDate);

        // Check for overlapping active leave
        final long overlappingCount = leaveRepository.countOverlappingActiveLeave(
            therapistProfileId,
            startDate,
            endDate
        );

        if (overlappingCount > 0) {
            throw new IllegalArgumentException(
                String.format("Leave request overlaps with %d existing active leave period(s)", overlappingCount)
            );
        }

        final var leaveRequest = new TherapistLeave(
            therapistProfileId,
            startDate,
            endDate,
            leaveType,
            requestNotes
        );

        final var saved = leaveRepository.save(leaveRequest);

        auditService.recordChange(therapistProfileId, "LEAVE", saved.getId(), "CREATE", null);

        return saved;
    }

    /**
     * Approves a pending leave request.
     *
     * <p>Approved leave immediately blocks the affected date range in the scheduling engine.
     *
     * @param leaveId leave request UUID
     * @param reviewerUserId admin user UUID
     * @param adminNotes optional admin notes
     * @return updated leave request with APPROVED status
     * @throws IllegalArgumentException if leave not found or not in PENDING status
     */
    @Transactional
    public TherapistLeave approveLeaveRequest(final UUID leaveId,
                                               final UUID reviewerUserId,
                                               final String adminNotes) {
        final var leave = leaveRepository.findById(leaveId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + leaveId));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException(
                "Leave request is not pending (current status: " + leave.getStatus() + ")"
            );
        }

        leave.approve(reviewerUserId, adminNotes);

        final var updated = leaveRepository.save(leave);

        auditService.recordChangeWithDetails(
            leave.getTherapistProfileId(),
            "LEAVE",
            leaveId,
            "APPROVE",
            new TherapistScheduleAuditService.FieldChange[]{
                new TherapistScheduleAuditService.FieldChange("status", "PENDING", "APPROVED"),
                new TherapistScheduleAuditService.FieldChange("reviewedBy", null, reviewerUserId.toString()),
                new TherapistScheduleAuditService.FieldChange("adminNotes", null, adminNotes)
            },
            null
        );

        return updated;
    }

    /**
     * Rejects a pending leave request.
     *
     * @param leaveId leave request UUID
     * @param reviewerUserId admin user UUID
     * @param adminNotes optional admin notes explaining rejection
     * @return updated leave request with REJECTED status
     * @throws IllegalArgumentException if leave not found or not in PENDING status
     */
    @Transactional
    public TherapistLeave rejectLeaveRequest(final UUID leaveId,
                                              final UUID reviewerUserId,
                                              final String adminNotes) {
        final var leave = leaveRepository.findById(leaveId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + leaveId));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException(
                "Leave request is not pending (current status: " + leave.getStatus() + ")"
            );
        }

        leave.reject(reviewerUserId, adminNotes);

        final var updated = leaveRepository.save(leave);

        auditService.recordChangeWithDetails(
            leave.getTherapistProfileId(),
            "LEAVE",
            leaveId,
            "REJECT",
            new TherapistScheduleAuditService.FieldChange[]{
                new TherapistScheduleAuditService.FieldChange("status", "PENDING", "REJECTED"),
                new TherapistScheduleAuditService.FieldChange("reviewedBy", null, reviewerUserId.toString()),
                new TherapistScheduleAuditService.FieldChange("adminNotes", null, adminNotes)
            },
            null
        );

        return updated;
    }

    /**
     * Cancels a leave request (can be done by therapist at any time).
     *
     * @param leaveId leave request UUID
     * @return updated leave request with CANCELLED status
     * @throws IllegalArgumentException if leave not found
     */
    @Transactional
    public TherapistLeave cancelLeaveRequest(final UUID leaveId) {
        final var leave = leaveRepository.findById(leaveId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + leaveId));

        final String oldStatus = leave.getStatus().toString();
        leave.cancel();

        final var updated = leaveRepository.save(leave);

        auditService.recordChangeWithDetails(
            leave.getTherapistProfileId(),
            "LEAVE",
            leaveId,
            "UPDATE",
            new TherapistScheduleAuditService.FieldChange[]{
                new TherapistScheduleAuditService.FieldChange("status", oldStatus, "CANCELLED")
            },
            null
        );

        return updated;
    }

    /**
     * Retrieves all leave periods for a therapist with a given status.
     *
     * @param therapistProfileId therapist profile UUID
     * @param status leave status to filter by
     * @return list of leave periods with the specified status
     */
    @Transactional(readOnly = true)
    public List<TherapistLeave> getLeaveByStatus(final UUID therapistProfileId, final LeaveStatus status) {
        return leaveRepository.findByTherapistProfileIdAndStatus(therapistProfileId, status);
    }

    /**
     * Retrieves all leave periods for a therapist, ordered by start date descending.
     *
     * @param therapistProfileId therapist profile UUID
     * @return list of all leave periods, most recent first
     */
    @Transactional(readOnly = true)
    public List<TherapistLeave> getAllLeave(final UUID therapistProfileId) {
        return leaveRepository.findByTherapistProfileIdOrderByStartDateDesc(therapistProfileId);
    }

    /**
     * Retrieves all pending leave requests across all therapists (admin view).
     *
     * @return list of pending leave requests, oldest first
     */
    @Transactional(readOnly = true)
    public List<TherapistLeave> getAllPendingLeaveRequests() {
        return leaveRepository.findByStatusOrderByRequestedAtAsc(LeaveStatus.PENDING);
    }

    /**
     * Finds all approved leave periods for a therapist that overlap with a given date range.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate query start date
     * @param endDate query end date
     * @return list of approved leave periods overlapping the date range
     */
    @Transactional(readOnly = true)
    public List<TherapistLeave> getApprovedLeaveInRange(final UUID therapistProfileId,
                                                         final LocalDate startDate,
                                                         final LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return leaveRepository.findApprovedLeaveOverlapping(therapistProfileId, startDate, endDate);
    }

    /**
     * Checks if a date range has any active (pending or approved) leave.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate query start date
     * @param endDate query end date
     * @return true if there is overlapping active leave
     */
    @Transactional(readOnly = true)
    public boolean hasActiveLeaveInRange(final UUID therapistProfileId,
                                          final LocalDate startDate,
                                          final LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return leaveRepository.countOverlappingActiveLeave(therapistProfileId, startDate, endDate) > 0;
    }

    // ========== Validation Helpers ==========

    private void validateDateRange(final LocalDate startDate, final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be on or before end date");
        }
    }

    private void validateNotPastDate(final LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot create leave request for past date: " + date);
        }
    }
}
