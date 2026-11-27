package com.psyassistant.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for user presence tracking shown to the global administrator.
 *
 * @param onlineWindow  a user counts as online when last seen within this window (default 5 minutes)
 * @param writeInterval minimum time between two {@code last_seen_at} writes per user (default 60 seconds)
 */
@ConfigurationProperties(prefix = "app.presence")
public record PresenceProperties(Duration onlineWindow, Duration writeInterval) {

    private static final Duration DEFAULT_ONLINE_WINDOW = Duration.ofMinutes(5);
    private static final Duration DEFAULT_WRITE_INTERVAL = Duration.ofSeconds(60);

    /** Applies defaults for unset values. */
    public PresenceProperties {
        onlineWindow = onlineWindow == null ? DEFAULT_ONLINE_WINDOW : onlineWindow;
        writeInterval = writeInterval == null ? DEFAULT_WRITE_INTERVAL : writeInterval;
    }
}
