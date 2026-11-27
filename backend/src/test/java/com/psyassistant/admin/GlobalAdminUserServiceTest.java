package com.psyassistant.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.auth.domain.RefreshTokenRepository;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.GlobalAdminProperties;
import com.psyassistant.common.config.PresenceProperties;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.users.SelfDeactivationException;
import com.psyassistant.users.User;
import com.psyassistant.users.UserHasHistoryException;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/** Unit tests for {@link GlobalAdminUserService}. */
class GlobalAdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final GlobalAdminUserService service = new GlobalAdminUserService(
            userRepository, organizationRepository, refreshTokenRepository, auditLogService,
            new GlobalAdminProperties("root@example.com"), new PresenceProperties(null, null));

    private User userWithEmail(final UUID id, final String email, final boolean active) {
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(email);
        when(user.isActive()).thenReturn(active);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void suspendDeactivatesUserRevokesTokensAndAudits() {
        UUID id = UUID.randomUUID();
        User user = userWithEmail(id, "u@example.com", true);

        service.suspend(id, UUID.randomUUID());

        verify(user).setActive(false);
        verify(refreshTokenRepository).deleteAllByUserId(id);
        verify(auditLogService).record(any());
    }

    @Test
    void suspendIsIdempotentForAlreadySuspendedUser() {
        UUID id = UUID.randomUUID();
        User user = userWithEmail(id, "u@example.com", false);

        service.suspend(id, UUID.randomUUID());

        verify(user, never()).setActive(false);
        verify(auditLogService, never()).record(any());
    }

    @Test
    void suspendRejectsSelfAndGlobalAdminAccount() {
        UUID self = UUID.randomUUID();
        userWithEmail(self, "me@example.com", true);
        UUID root = UUID.randomUUID();
        userWithEmail(root, "ROOT@example.com", true);

        assertThatThrownBy(() -> service.suspend(self, self)).isInstanceOf(SelfDeactivationException.class);
        assertThatThrownBy(() -> service.suspend(root, UUID.randomUUID()))
                .isInstanceOf(SelfDeactivationException.class);
        verify(refreshTokenRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void suspendUnknownUserThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspend(id, UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void reactivateActivatesSuspendedUser() {
        UUID id = UUID.randomUUID();
        User user = userWithEmail(id, "u@example.com", false);

        service.reactivate(id, UUID.randomUUID());

        verify(user).setActive(true);
        verify(auditLogService).record(any());
    }

    @Test
    void deleteRemovesUserWithoutHistory() {
        UUID id = UUID.randomUUID();
        User user = userWithEmail(id, "u@example.com", true);

        service.delete(id, UUID.randomUUID());

        verify(userRepository).delete(user);
        verify(userRepository).flush();
        verify(auditLogService).record(any());
    }

    @Test
    void deleteMapsIntegrityViolationToUserHasHistory() {
        UUID id = UUID.randomUUID();
        userWithEmail(id, "u@example.com", true);
        doThrow(new DataIntegrityViolationException("fk")).when(userRepository).flush();

        assertThatThrownBy(() -> service.delete(id, UUID.randomUUID()))
                .isInstanceOf(UserHasHistoryException.class);
        verify(auditLogService, never()).record(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void enrichesUsersWithOrganizationAndToleratesMissingOrg() {
        UUID orgId = UUID.randomUUID();
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);
        when(org.getName()).thenReturn("Acme Practice");
        when(org.getType()).thenReturn(OrganizationType.SOLO);

        User withOrg = mock(User.class);
        when(withOrg.getOrgId()).thenReturn(orgId);
        when(withOrg.getEmail()).thenReturn("a@example.com");
        User withoutOrg = mock(User.class);
        when(withoutOrg.getOrgId()).thenReturn(null);
        when(withoutOrg.getEmail()).thenReturn("b@example.com");

        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withOrg, withoutOrg), pageable, 2));
        when(organizationRepository.findAllById(anyIterable())).thenReturn(List.of(org));

        GlobalUserPageResponse result = service.listUsers(null, null, null, pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content().get(0).orgName()).isEqualTo("Acme Practice");
        assertThat(result.content().get(0).orgType()).isEqualTo(OrganizationType.SOLO);
        assertThat(result.content().get(1).orgName()).isNull();
    }

    private GlobalUserSummaryDto listSingle(final User user) {
        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(organizationRepository.findAllById(anyIterable())).thenReturn(List.of());
        return service.listUsers(null, null, null, pageable).content().get(0);
    }

    private User presenceUser(final boolean active, final Instant lockedUntil, final Instant lastSeen) {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(active);
        when(user.getLockedUntil()).thenReturn(lockedUntil);
        when(user.getLastSeenAt()).thenReturn(lastSeen);
        return user;
    }

    @Test
    void presenceIsOnlineWhenSeenWithinWindow() {
        GlobalUserSummaryDto dto = listSingle(presenceUser(true, null, Instant.now().minusSeconds(60)));

        assertThat(dto.presence()).isEqualTo(UserPresence.ONLINE);
        assertThat(dto.blockReason()).isNull();
    }

    @Test
    void presenceIsOfflineWhenSeenLongAgoOrNever() {
        assertThat(listSingle(presenceUser(true, null, Instant.now().minusSeconds(3600))).presence())
                .isEqualTo(UserPresence.OFFLINE);
        assertThat(listSingle(presenceUser(true, null, null)).presence()).isEqualTo(UserPresence.OFFLINE);
    }

    @Test
    void suspendedUserIsBlockedEvenIfRecentlySeen() {
        GlobalUserSummaryDto dto = listSingle(presenceUser(false, null, Instant.now()));

        assertThat(dto.presence()).isEqualTo(UserPresence.BLOCKED);
        assertThat(dto.blockReason()).isEqualTo(BlockReason.SUSPENDED);
    }

    @Test
    void lockedUserIsBlockedWithLockedReason() {
        GlobalUserSummaryDto dto = listSingle(presenceUser(true, Instant.now().plusSeconds(300), Instant.now()));

        assertThat(dto.presence()).isEqualTo(UserPresence.BLOCKED);
        assertThat(dto.blockReason()).isEqualTo(BlockReason.LOCKED);
    }

    @Test
    void expiredLockDoesNotBlock() {
        GlobalUserSummaryDto dto = listSingle(presenceUser(true, Instant.now().minusSeconds(5), Instant.now()));

        assertThat(dto.presence()).isEqualTo(UserPresence.ONLINE);
    }

    @Test
    void statsAggregateCountsFromRepository() {
        when(userRepository.countAllUsers()).thenReturn(10L);
        when(userRepository.countOnline(any(), any())).thenReturn(3L);
        when(userRepository.countBlocked(any())).thenReturn(2L);

        GlobalUserStatsDto stats = service.getStats();

        assertThat(stats).isEqualTo(new GlobalUserStatsDto(10, 3, 2));
    }
}
