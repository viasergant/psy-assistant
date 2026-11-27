package com.psyassistant.auth.service;

import com.psyassistant.common.config.PresenceProperties;
import com.psyassistant.users.UserRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Records the last authenticated activity of users for the presence marker.
 *
 * <p>Writes are throttled per user (default one per minute) using an in-memory map, so the cost
 * is bounded regardless of request volume. Failures never affect request authentication.
 */
@Component
public class UserActivityTracker {

    private static final Logger LOG = LoggerFactory.getLogger(UserActivityTracker.class);

    private final UserRepository userRepository;
    private final PresenceProperties properties;
    private final ConcurrentHashMap<UUID, Instant> lastWrite = new ConcurrentHashMap<>();

    /**
     * Constructs the tracker.
     *
     * @param userRepository user repository
     * @param properties     presence settings
     */
    public UserActivityTracker(final UserRepository userRepository, final PresenceProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
    }

    /**
     * Notes that the user just made an authenticated request.
     *
     * @param userId the user's UUID
     */
    public void recordActivity(final UUID userId) {
        Instant now = Instant.now();
        Instant threshold = now.minus(properties.writeInterval());
        boolean[] write = {false};
        lastWrite.compute(userId, (id, previous) -> {
            if (previous == null || !previous.isAfter(threshold)) {
                write[0] = true;
                return now;
            }
            return previous;
        });
        if (!write[0]) {
            return;
        }
        try {
            userRepository.touchLastSeen(userId, now);
        } catch (RuntimeException ex) {
            lastWrite.remove(userId);
            LOG.debug("event=LAST_SEEN_UPDATE_FAILED userId={} reason={}", userId, ex.getClass().getSimpleName());
        }
    }
}
