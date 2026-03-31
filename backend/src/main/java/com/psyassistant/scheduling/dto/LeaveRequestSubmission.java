package com.psyassistant.scheduling.dto;

import com.psyassistant.scheduling.domain.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request DTO for submitting a leave request.
 */
public record LeaveRequestSubmission(

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @NotNull(message = "Leave type is required")
    LeaveType leaveType,

    String requestNotes  // Optional therapist notes
) {
}
