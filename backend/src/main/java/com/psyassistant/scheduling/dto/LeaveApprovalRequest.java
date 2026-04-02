package com.psyassistant.scheduling.dto;

/**
 * Request DTO for approving or rejecting a leave request.
 * The reviewer user ID is automatically extracted from the authenticated user's JWT token.
 */
public record LeaveApprovalRequest(

    String adminNotes  // Optional admin notes
) {
}
