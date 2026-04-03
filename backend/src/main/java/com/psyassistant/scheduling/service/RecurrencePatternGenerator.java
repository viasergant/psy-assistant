package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.RecurrenceType;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Generates the ordered list of {@link ZonedDateTime} start times for a recurring series.
 *
 * <p><strong>DST-safe</strong>: uses {@code ZonedDateTime.plusWeeks()}, {@code plusMonths()},
 * etc., which preserve the wall-clock time across daylight saving transitions by normalising
 * in the zone's own rules (Java {@code ZoneRules}).
 *
 * <p><strong>Hard cap</strong>: {@code count} is constrained to [1, 20] by the callers
 * (validated at the DTO layer), so this component does not re-validate.
 */
@Component
public class RecurrencePatternGenerator {

    /**
     * Generates {@code count} occurrence start times starting from {@code anchor}.
     *
     * <p>The first element is always {@code anchor} itself (index 0).
     *
     * @param anchor first occurrence's start time (including timezone)
     * @param type recurrence spacing
     * @param count total number of occurrences to generate (1-20)
     * @return immutable list of {@code count} {@link ZonedDateTime} values
     */
    public List<ZonedDateTime> generate(final ZonedDateTime anchor,
                                         final RecurrenceType type,
                                         final int count) {
        final List<ZonedDateTime> slots = new ArrayList<>(count);
        ZonedDateTime current = anchor;

        for (int i = 0; i < count; i++) {
            slots.add(current);
            current = advance(current, type);
        }

        return List.copyOf(slots);
    }

    private ZonedDateTime advance(final ZonedDateTime from, final RecurrenceType type) {
        return switch (type) {
            case WEEKLY -> from.plusWeeks(1);
            case BIWEEKLY -> from.plusWeeks(2);
            case MONTHLY -> from.plusMonths(1);
        };
    }
}
