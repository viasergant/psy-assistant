package com.psyassistant.scheduling.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response for the recurring conflict pre-flight check.
 *
 * <p>Contains one {@link RecurringSlotCheckResult} per generated occurrence so the
 * UI can render a slot table with per-row conflict indicators.
 */
public record RecurringConflictCheckResponse(
        /** One entry per generated occurrence (ordered by recurrenceIndex). */
        List<RecurringSlotCheckResult> generatedSlots,

        /** Number of slots that have at least one conflict. */
        int conflictCount,

        /** Number of slots with no conflicts. */
        int cleanSlotCount
) {

    /**
     * Result for a single generated occurrence slot.
     *
     * @param index zero-based occurrence index within the series
     * @param startTime wall-clock start time of this occurrence
     * @param hasConflict whether at least one existing appointment overlaps this slot
     * @param conflictDetails details of the first conflicting appointment (null if no conflict)
     */
    public record RecurringSlotCheckResult(
            int index,
            ZonedDateTime startTime,
            boolean hasConflict,
            ConflictDetail conflictDetails
    ) {
    }

    /**
     * Minimal information about a conflicting appointment for UI display.
     *
     * @param appointmentId conflicting appointment UUID
     * @param clientName display name of the client (may be null if join not available)
     * @param startTime start time of the conflicting appointment
     */
    public record ConflictDetail(
            UUID appointmentId,
            String clientName,
            ZonedDateTime startTime
    ) {
    }
}
