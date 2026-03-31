package com.psyassistant.scheduling.domain;

/**
 * Status of a therapist leave request.
 */
public enum LeaveStatus {
    
    /** Leave request submitted and awaiting administrator review. */
    PENDING,
    
    /** Leave request approved by administrator; dates are blocked in schedule. */
    APPROVED,
    
    /** Leave request rejected by administrator; dates remain available. */
    REJECTED,
    
    /** Leave request cancelled by therapist before or after approval. */
    CANCELLED
}
