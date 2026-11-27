package com.psyassistant.users;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.common.tenant.TenantContext;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Verifies the presence/stat queries on {@link UserRepository} against H2, across two organizations. */
@SpringBootTest
@ActiveProfiles("test")
class UserPresenceQueriesIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private final UUID orgA = UUID.randomUUID();
    private final UUID orgB = UUID.randomUUID();
    private long baseTotal;
    private long baseBlocked;
    private long baseOnline;

    @BeforeEach
    void snapshotBaseline() {
        Instant now = Instant.now();
        baseTotal = userRepository.countAllUsers();
        baseBlocked = userRepository.countBlocked(now);
        baseOnline = userRepository.countOnline(now, now.minusSeconds(300));
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        userRepository.deleteAll(userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@presence.test")).toList());
    }

    private User save(final String name, final UUID org, final boolean active, final Instant lockedUntil) {
        User user = new User(name + "@presence.test", "hash", Set.of(UserRole.THERAPIST), active);
        user.setOrgId(org);
        user.setLockedUntil(lockedUntil);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void countsOnlineAndBlockedAcrossOrganizations() {
        Instant now = Instant.now();
        User onlineA = save("online-a", orgA, true, null);
        User onlineB = save("online-b", orgB, true, null);
        User stale = save("stale", orgA, true, null);
        User suspended = save("suspended", orgB, false, null);
        User locked = save("locked", orgA, true, now.plusSeconds(600));
        User suspendedButSeen = save("suspended-seen", orgB, false, null);

        userRepository.touchLastSeen(onlineA.getId(), now);
        userRepository.touchLastSeen(onlineB.getId(), now.minusSeconds(30));
        userRepository.touchLastSeen(stale.getId(), now.minusSeconds(3600));
        userRepository.touchLastSeen(locked.getId(), now);
        userRepository.touchLastSeen(suspendedButSeen.getId(), now);

        TenantContext.set(orgA); // a bound tenant must not narrow the global counts
        assertThat(userRepository.countAllUsers()).isEqualTo(baseTotal + 6);
        assertThat(userRepository.countBlocked(now)).isEqualTo(baseBlocked + 3);
        assertThat(userRepository.countOnline(now, now.minusSeconds(300))).isEqualTo(baseOnline + 2);
        assertThat(suspended.getId()).isNotNull();
    }

    @Test
    void touchLastSeenStoresTimestampWithoutChangingUpdatedAt() {
        User user = save("touch", orgA, true, null);
        Instant updatedAtBefore = userRepository.findById(user.getId()).orElseThrow().getUpdatedAt();
        Instant seen = Instant.now().minusSeconds(10);

        userRepository.touchLastSeen(user.getId(), seen);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenAt()).isEqualTo(seen);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAtBefore);
    }
}
