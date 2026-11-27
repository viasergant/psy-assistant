package com.psyassistant.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.psyassistant.auth.service.UserActivityTracker;
import com.psyassistant.common.config.PresenceProperties;
import com.psyassistant.users.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UserActivityTracker}. */
class UserActivityTrackerTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    void writesOnFirstCallAndSkipsWithinInterval() {
        UserActivityTracker tracker = new UserActivityTracker(userRepository, new PresenceProperties(null, null));
        UUID id = UUID.randomUUID();

        tracker.recordActivity(id);
        tracker.recordActivity(id);
        tracker.recordActivity(id);

        verify(userRepository, times(1)).touchLastSeen(eq(id), any(Instant.class));
    }

    @Test
    void writesAgainOnceIntervalElapsed() {
        UserActivityTracker tracker = new UserActivityTracker(
                userRepository, new PresenceProperties(null, Duration.ZERO));
        UUID id = UUID.randomUUID();

        tracker.recordActivity(id);
        tracker.recordActivity(id);

        verify(userRepository, times(2)).touchLastSeen(eq(id), any(Instant.class));
    }

    @Test
    void tracksUsersIndependently() {
        UserActivityTracker tracker = new UserActivityTracker(userRepository, new PresenceProperties(null, null));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        tracker.recordActivity(a);
        tracker.recordActivity(b);

        verify(userRepository).touchLastSeen(eq(a), any(Instant.class));
        verify(userRepository).touchLastSeen(eq(b), any(Instant.class));
    }

    @Test
    void repositoryFailureDoesNotPropagateAndAllowsRetry() {
        UserActivityTracker tracker = new UserActivityTracker(userRepository, new PresenceProperties(null, null));
        UUID id = UUID.randomUUID();
        doThrow(new IllegalStateException("db down")).when(userRepository).touchLastSeen(eq(id), any());

        tracker.recordActivity(id);
        tracker.recordActivity(id);

        verify(userRepository, times(2)).touchLastSeen(eq(id), any(Instant.class));
    }
}
