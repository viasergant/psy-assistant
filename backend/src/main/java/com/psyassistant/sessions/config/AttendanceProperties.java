package com.psyassistant.sessions.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for attendance tracking and no-show management.
 *
 * <p>Bound to the {@code attendance} YAML namespace.
 */
@ConfigurationProperties(prefix = "app.attendance")
public record AttendanceProperties(
        NoShowProperties noShow,
        LateCancellationProperties lateCancellation,
        FollowupProperties followup
) {

    /**
     * No-show threshold and lookback window settings.
     *
     * @param threshold    number of no-shows before a client is flagged as at-risk
     * @param lookbackDays rolling window in days for counting no-shows; 0 means all-time
     */
    public record NoShowProperties(int threshold, int lookbackDays) {
    }

    /**
     * Late cancellation policy settings.
     *
     * @param windowHours hours before session start within which a cancellation is "late"
     */
    public record LateCancellationProperties(int windowHours) {
    }

    /**
     * Follow-up task assignment settings.
     *
     * @param assignTo who to assign the follow-up task to: THERAPIST or ADMIN
     */
    public record FollowupProperties(String assignTo) {
    }
}
