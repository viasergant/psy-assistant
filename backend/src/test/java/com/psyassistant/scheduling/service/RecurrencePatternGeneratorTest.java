package com.psyassistant.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.scheduling.domain.RecurrenceType;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RecurrencePatternGenerator}.
 *
 * <p>Verifies DST-safe slot generation for all three recurrence types
 * and the 20-occurrence hard cap contract.
 */
@DisplayName("RecurrencePatternGenerator Tests")
class RecurrencePatternGeneratorTest {

    private RecurrencePatternGenerator generator;

    private static final ZoneId NYC = ZoneId.of("America/New_York");
    private static final ZonedDateTime ANCHOR =
            ZonedDateTime.of(2026, 4, 1, 10, 0, 0, 0, NYC);

    @BeforeEach
    void setUp() {
        generator = new RecurrencePatternGenerator();
    }

    // ========== WEEKLY ==========

    @Test
    @DisplayName("Weekly: generates correct count")
    void weeklyGeneratesCorrectCount() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.WEEKLY, 4);
        assertThat(slots).hasSize(4);
    }

    @Test
    @DisplayName("Weekly: first slot equals anchor")
    void weeklyFirstSlotIsAnchor() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.WEEKLY, 3);
        assertThat(slots.get(0)).isEqualTo(ANCHOR);
    }

    @Test
    @DisplayName("Weekly: subsequent slots are 7 days apart")
    void weeklySpacing() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.WEEKLY, 3);
        assertThat(slots.get(1).toInstant())
                .isEqualTo(ANCHOR.plusWeeks(1).toInstant());
        assertThat(slots.get(2).toInstant())
                .isEqualTo(ANCHOR.plusWeeks(2).toInstant());
    }

    @Test
    @DisplayName("Weekly: preserves wall-clock time across DST spring-forward")
    void weeklyPreservesWallClockAcrossDst() {
        // 2026-03-08 is spring-forward in US/Eastern
        final ZonedDateTime preDst =
                ZonedDateTime.of(2026, 3, 4, 10, 0, 0, 0, NYC); // before spring-forward
        final List<ZonedDateTime> slots = generator.generate(preDst, RecurrenceType.WEEKLY, 3);

        // All slots must be at 10:00 local time
        for (final ZonedDateTime slot : slots) {
            assertThat(slot.getHour()).isEqualTo(10);
            assertThat(slot.getMinute()).isEqualTo(0);
        }
    }

    // ========== BIWEEKLY ==========

    @Test
    @DisplayName("Biweekly: generates correct count")
    void biweeklyGeneratesCorrectCount() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.BIWEEKLY, 5);
        assertThat(slots).hasSize(5);
    }

    @Test
    @DisplayName("Biweekly: subsequent slots are 14 days apart")
    void biweeklySpacing() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.BIWEEKLY, 3);
        assertThat(slots.get(1).toInstant())
                .isEqualTo(ANCHOR.plusWeeks(2).toInstant());
        assertThat(slots.get(2).toInstant())
                .isEqualTo(ANCHOR.plusWeeks(4).toInstant());
    }

    // ========== MONTHLY ==========

    @Test
    @DisplayName("Monthly: generates correct count")
    void monthlyGeneratesCorrectCount() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.MONTHLY, 6);
        assertThat(slots).hasSize(6);
    }

    @Test
    @DisplayName("Monthly: subsequent slots are 1 month apart")
    void monthlySpacing() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.MONTHLY, 3);
        assertThat(slots.get(1).getMonthValue()).isEqualTo(ANCHOR.getMonthValue() + 1);
        assertThat(slots.get(1).getDayOfMonth()).isEqualTo(ANCHOR.getDayOfMonth());
    }

    @Test
    @DisplayName("Monthly: preserves wall-clock time")
    void monthlyPreservesWallClock() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.MONTHLY, 12);
        for (final ZonedDateTime slot : slots) {
            assertThat(slot.getHour()).isEqualTo(10);
            assertThat(slot.getMinute()).isEqualTo(0);
        }
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Count of 1 returns only the anchor slot")
    void countOneReturnsSingleSlot() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.WEEKLY, 1);
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0)).isEqualTo(ANCHOR);
    }

    @Test
    @DisplayName("Count of 20 returns exactly 20 slots (hard cap)")
    void countTwentyReturnsExactlyTwenty() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.WEEKLY, 20);
        assertThat(slots).hasSize(20);
    }

    @Test
    @DisplayName("Returned list is immutable")
    void returnedListIsImmutable() {
        final List<ZonedDateTime> slots = generator.generate(ANCHOR, RecurrenceType.WEEKLY, 3);
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> slots.add(ANCHOR)
        );
    }
}
