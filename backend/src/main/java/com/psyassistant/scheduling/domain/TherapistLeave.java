package com.psyassistant.scheduling.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a therapist leave period request.
 *
 * <p>Leave requests go through an approval workflow:
 * <ol>
 *     <li>Therapist submits request → status = PENDING</li>
 *     <li>Admin reviews and approves/rejects → status = APPROVED or REJECTED</li>
 *     <li>Therapist may cancel before or after approval → status = CANCELLED</li>
 * </ol>
 *
 * <p>Approved leave blocks all dates in the range [{@code startDate}, {@code endDate}] from scheduling.
 *
 * <p>Extends {@link BaseEntity} to inherit UUID primary key and Spring Data Auditing fields.
 */
@Entity
@Table(name = "therapist_leave")
public class TherapistLeave extends BaseEntity {

    /** Foreign key to the therapist profile. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** Start date of the leave period (inclusive). */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** End date of the leave period (inclusive). */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Type of leave (annual, sick, public holiday, other). */
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 32)
    private LeaveType leaveType;

    /** Current status of the leave request (pending, approved, rejected, cancelled). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LeaveStatus status = LeaveStatus.PENDING;

    /** Optional notes provided by the therapist when submitting the request. */
    @Column(name = "request_notes", columnDefinition = "TEXT")
    private String requestNotes;

    /** Optional notes provided by the admin when reviewing the request. */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /** Timestamp when the leave request was initially submitted. */
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    /** Timestamp when the admin approved or rejected the request. */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** User ID of the administrator who reviewed this request. */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    /**
     * Default constructor for JPA.
     */
    protected TherapistLeave() {
    }

    /**
     * Creates a new leave request with PENDING status.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param leaveType type of leave
     * @param requestNotes optional therapist notes
     */
    public TherapistLeave(final UUID therapistProfileId,
                          final LocalDate startDate,
                          final LocalDate endDate,
                          final LeaveType leaveType,
                          final String requestNotes) {
        this.therapistProfileId = therapistProfileId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveType = leaveType;
        this.requestNotes = requestNotes;
        this.status = LeaveStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    /**
     * Approves the leave request.
     *
     * @param reviewerUserId admin user ID
     * @param adminNotes optional admin notes
     */
    public void approve(final UUID reviewerUserId, final String adminNotes) {
        this.status = LeaveStatus.APPROVED;
        this.reviewedBy = reviewerUserId;
        this.adminNotes = adminNotes;
        this.reviewedAt = Instant.now();
    }

    /**
     * Rejects the leave request.
     *
     * @param reviewerUserId admin user ID
     * @param adminNotes optional admin notes explaining rejection
     */
    public void reject(final UUID reviewerUserId, final String adminNotes) {
        this.status = LeaveStatus.REJECTED;
        this.reviewedBy = reviewerUserId;
        this.adminNotes = adminNotes;
        this.reviewedAt = Instant.now();
    }

    /**
     * Cancels the leave request (can be done by therapist at any time).
     */
    public void cancel() {
        this.status = LeaveStatus.CANCELLED;
    }

    // Getters and setters

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(final UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(final LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(final LeaveStatus status) {
        this.status = status;
    }

    public String getRequestNotes() {
        return requestNotes;
    }

    public void setRequestNotes(final String requestNotes) {
        this.requestNotes = requestNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(final String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(final Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(final UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
