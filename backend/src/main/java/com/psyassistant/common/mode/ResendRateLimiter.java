package com.psyassistant.common.mode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Bounded in-memory sliding-window limiter for confirmation-email resends.
 *
 * <p>Per-instance only: with several backend instances the effective limit is multiplied.
 * The map is capped so a flood of distinct keys cannot exhaust memory.
 */
@Component
@Profile("saas")
public class ResendRateLimiter {

    static final int MAX_PER_WINDOW = 3;
    static final Duration WINDOW = Duration.ofHours(1);
    static final int MAX_KEYS = 10_000;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    /** Production constructor. */
    public ResendRateLimiter() {
        this(Clock.systemUTC());
    }

    ResendRateLimiter(final Clock clock) {
        this.clock = clock;
    }

    /**
     * Records an attempt and reports whether it is within the limit.
     *
     * @param key opaque identifier (hash of email, or IP address)
     * @return true if allowed, false if the key exceeded the window limit
     */
    public boolean tryAcquire(final String key) {
        Instant now = clock.instant();
        if (hits.size() >= MAX_KEYS && !hits.containsKey(key)) {
            evictExpired(now);
            if (hits.size() >= MAX_KEYS) {
                return false;
            }
        }
        Deque<Instant> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && !window.peekFirst().plus(WINDOW).isAfter(now)) {
                window.pollFirst();
            }
            if (window.size() >= MAX_PER_WINDOW) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private void evictExpired(final Instant now) {
        Iterator<Map.Entry<String, Deque<Instant>>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Instant> window = it.next().getValue();
            synchronized (window) {
                if (window.isEmpty() || !window.peekLast().plus(WINDOW).isAfter(now)) {
                    it.remove();
                }
            }
        }
    }
}
