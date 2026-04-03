package com.psyassistant.scheduling.dto;

import java.util.List;

/**
 * Response DTO for recurring series creation.
 *
 * <p>Reports exactly how many occurrences were saved vs. skipped so the caller
 * can show an appropriate summary notification.
 */
public record CreateRecurringSeriesResponse(
        /** ID of the newly created {@code AppointmentSeries} record. */
        Long seriesId,

        /** Number of occurrences originally requested. */
        int requestedOccurrences,

        /** Number of appointment rows actually persisted. */
        int savedOccurrences,

        /** Number of slots skipped due to conflicts (always 0 when resolution=ABORT). */
        int skippedOccurrences,

        /** Full appointment details for every saved occurrence. */
        List<AppointmentResponse> appointments,

        /** Waitlist entry IDs created for skipped slots (stub for PA-34). */
        List<Long> waitlistEntryIds
) {
}
