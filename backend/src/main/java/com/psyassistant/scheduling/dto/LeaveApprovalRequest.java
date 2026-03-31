package com.psyassistant.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for approving or rejecting a leave request.
 */
public record LeaveApprovalRequest(

    @NotNull(message = "Reviewer user ID is required")
    UUID reviewerUserId,

    String adminNotes  // Optional admin notes
) {
}
