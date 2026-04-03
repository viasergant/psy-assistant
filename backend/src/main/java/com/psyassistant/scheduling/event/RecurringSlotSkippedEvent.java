package com.psyassistant.scheduling.event;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Published when a recurring appointment slot is skipped because of a conflict.
 *
 * <p>Downstream listeners (PA-34 waitlist service) subscribe to this event to
 * create a waitlist entry for the affected therapist / client / time combination.
 *
 * <p>The event is published within the same transaction that creates the series,
 * so listeners using {@code @TransactionalEventListener} with
 * {@code AFTER_COMMIT} phase will only receive it on a successful commit.
 *
 * @param seriesId parent series ID (can be used to trace back the original request)
 * @param therapistProfileId therapist UUID
 * @param clientId client UUID
 * @param slotTime wall-clock start time of the skipped slot
 * @param durationMinutes duration of the intended appointment
 * @param recurrenceIndex zero-based index of this slot within the series
 */
public record RecurringSlotSkippedEvent(
        Long seriesId,
        UUID therapistProfileId,
        UUID clientId,
        ZonedDateTime slotTime,
        int durationMinutes,
        int recurrenceIndex
) {
}
