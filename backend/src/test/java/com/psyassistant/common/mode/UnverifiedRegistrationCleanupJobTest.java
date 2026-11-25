package com.psyassistant.common.mode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.auth.token.OneTimeTokenRepository;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UnverifiedRegistrationCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-11-25T03:30:00Z");

    private UserRepository users;
    private OrganizationRepository orgs;
    private OneTimeTokenRepository tokens;
    private UnverifiedRegistrationCleanupJob job;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        orgs = mock(OrganizationRepository.class);
        tokens = mock(OneTimeTokenRepository.class);
        job = new UnverifiedRegistrationCleanupJob(users, orgs, tokens, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void purgeStaleNothingStaleDoesNothing() {
        when(users.findAllByEmailVerifiedFalseAndCreatedAtBefore(any())).thenReturn(List.of());

        assertThat(job.purgeStale()).isZero();
        verify(tokens, never()).deleteAllByUserIdIn(any());
    }

    @Test
    void purgeStaleUsesSevenDayCutoffAndDeletesEmptyOrg() {
        UUID orgId = UUID.randomUUID();
        User user = staleUser(orgId);
        when(users.findAllByEmailVerifiedFalseAndCreatedAtBefore(NOW.minus(UnverifiedRegistrationCleanupJob.RETENTION)))
                .thenReturn(List.of(user));
        when(users.countByOrgId(orgId)).thenReturn(0L);

        assertThat(job.purgeStale()).isEqualTo(1);

        verify(tokens).deleteAllByUserIdIn(any());
        verify(users).delete(user);
        verify(orgs).deleteById(orgId);
    }

    @Test
    void purgeStaleOrgWithOtherUsersIsKept() {
        UUID orgId = UUID.randomUUID();
        when(users.findAllByEmailVerifiedFalseAndCreatedAtBefore(any()))
                .thenReturn(List.of(staleUser(orgId)));
        when(users.countByOrgId(orgId)).thenReturn(1L);

        job.purgeStale();

        verify(orgs, never()).deleteById(any());
    }

    private User staleUser(final UUID orgId) {
        User user = new User("a@example.com", "h", Set.of(UserRole.THERAPIST), false);
        user.setEmailVerified(false);
        user.setOrgId(orgId);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
