package com.psyassistant.scheduling.domain;

/**
 * Types of leave that a therapist can request.
 */
public enum LeaveType {
    
    /** Annual vacation leave. */
    ANNUAL,
    
    /** Sick leave for health-related absence. */
    SICK,
    
    /** Public holiday or statutory holiday. */
    PUBLIC_HOLIDAY,
    
    /** Other types of leave not covered by the above categories. */
    OTHER
}
