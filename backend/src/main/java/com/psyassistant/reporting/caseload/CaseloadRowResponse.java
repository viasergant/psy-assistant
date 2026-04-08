package com.psyassistant.reporting.caseload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a single row in the caseload overview table.
 *
 * <p>All nullable fields ({@code contractedHoursPerWeek}, {@code utilizationRate})
 * are null when the therapist has not set contracted hours.
 *
 * @param therapistProfileId    UUID of the therapist_profile record
 * @param therapistName         full name of the therapist
 * @param activeClientCount     number of active clients
 * @param sessionsThisWeek      sessions scheduled/completed in the current ISO week
 * @param sessionsThisMonth     sessions scheduled/completed in the current calendar month
 * @param scheduledHoursThisWeek total scheduled hours in the current ISO week
 * @param contractedHoursPerWeek contracted hours per week, or {@code null} if not set
 * @param utilizationRate       scheduled / contracted ratio, or {@code null} if not computable
 */
public record CaseloadRowResponse(
    UUID therapistProfileId,
    String therapistName,
    int activeClientCount,
    int sessionsThisWeek,
    int sessionsThisMonth,
    BigDecimal scheduledHoursThisWeek,
    BigDecimal contractedHoursPerWeek,
    BigDecimal utilizationRate
) {
}
