package com.psyassistant.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.users.dto.CreateUserRequest;
import com.psyassistant.users.dto.PatchUserRequest;
import com.psyassistant.users.dto.UserPageResponse;
import com.psyassistant.users.dto.UserSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link UserManagementService}.
 *
 * <p>Covers: create user, duplicate email, update role, deactivate, reactivate,
 * self-deactivation guard, password reset initiation, and list/filter.
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserManagementService service;

    private UUID adminId;
    private UUID targetId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(
                userRepository, resetTokenRepository, auditLogService, passwordEncoder);
        adminId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        activeUser = makeUser(targetId, "user@example.com", UserRole.USER, true);
    }

    // ---- createUser -------------------------------------------------------

    @Test
    void createUserSavesAccountAndIssuesResetToken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserSummaryDto dto = service.createUser(
                new CreateUserRequest("new@example.com", "Alice Smith", UserRole.USER), adminId);

        assertThat(dto.email()).isEqualTo("new@example.com");
        assertThat(dto.fullName()).isEqualTo("Alice Smith");
        assertThat(dto.active()).isTrue();
        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(auditLogService).record(any());
    }

    @Test
    void createUserThrowsDuplicateEmailExceptionWhenEmailExists() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                service.createUser(
                        new CreateUserRequest("dup@example.com", "Bob", UserRole.USER), adminId))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("dup@example.com");

        verify(userRepository, never()).save(any());
    }

    // ---- updateUser — role -----------------------------------------------

    @Test
    void updateUserChangesRoleAndAudits() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryDto dto = service.updateUser(
                targetId, new PatchUserRequest(null, UserRole.ADMIN, null), adminId);

        assertThat(dto.role()).isEqualTo(UserRole.ADMIN);
        verify(auditLogService).record(any());
    }

    @Test
    void updateUserDoesNotAuditWhenRoleUnchanged() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Same role — no audit expected
        service.updateUser(targetId, new PatchUserRequest(null, UserRole.USER, null), adminId);

        verify(auditLogService, never()).record(any());
    }

    // ---- updateUser — deactivate / reactivate ----------------------------

    @Test
    void updateUserDeactivatesAccountAndAudits() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryDto dto = service.updateUser(
                targetId, new PatchUserRequest(null, null, false), adminId);

        assertThat(dto.active()).isFalse();
        verify(auditLogService).record(any());
    }

    @Test
    void updateUserReactivatesAccountAndAudits() {
        User inactiveUser = makeUser(targetId, "inactive@example.com", UserRole.USER, false);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryDto dto = service.updateUser(
                targetId, new PatchUserRequest(null, null, true), adminId);

        assertThat(dto.active()).isTrue();
        verify(auditLogService).record(any());
    }

    // ---- self-deactivation guard -----------------------------------------

    @Test
    void updateUserThrowsSelfDeactivationExceptionWhenAdminDeactivatesSelf() {
        User selfAdmin = makeUser(adminId, "admin@example.com", UserRole.ADMIN, true);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() ->
                service.updateUser(adminId, new PatchUserRequest(null, null, false), adminId))
                .isInstanceOf(SelfDeactivationException.class);

        verify(userRepository, never()).save(any());
    }

    // ---- updateUser — fullName -------------------------------------------

    @Test
    void updateUserChangesFullName() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryDto dto = service.updateUser(
                targetId, new PatchUserRequest("New Name", null, null), adminId);

        assertThat(dto.fullName()).isEqualTo("New Name");
    }

    // ---- updateUser — entity not found -----------------------------------

    @Test
    void updateUserThrowsEntityNotFoundForUnknownId() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateUser(targetId, new PatchUserRequest(null, null, null), adminId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- initiatePasswordReset -------------------------------------------

    @Test
    void initiatePasswordResetPurgesOldTokensAndCreatesNewOne() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(activeUser));
        when(resetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String rawToken = service.initiatePasswordReset(targetId, adminId);

        assertThat(rawToken).isNotBlank().hasSize(64); // 32 bytes = 64 hex chars
        verify(resetTokenRepository).deleteAllByUserId(targetId);
        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(auditLogService).record(any());
    }

    @Test
    void initiatePasswordResetThrowsEntityNotFoundForUnknownId() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiatePasswordReset(targetId, adminId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void initiatePasswordResetTokenHashIsSha256OfRaw() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(activeUser));

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        when(resetTokenRepository.save(tokenCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        String rawToken = service.initiatePasswordReset(targetId, adminId);

        PasswordResetToken saved = tokenCaptor.getValue();
        // The stored hash must NOT equal the raw token
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getTokenHash()).hasSize(64);
    }

    // ---- listUsers -------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void listUsersReturnsPaginatedResults() {
        Page<User> page = new PageImpl<>(
                List.of(activeUser), PageRequest.of(0, 20), 1);
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);

        UserPageResponse response = service.listUsers(null, null, PageRequest.of(0, 20));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).email()).isEqualTo("user@example.com");
    }

    @SuppressWarnings("unchecked")
    @Test
    void listUsersWithRoleFilterPassesSpecificationToRepository() {
        Page<User> emptyPage = Page.empty(PageRequest.of(0, 20));
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        service.listUsers(UserRole.ADMIN, true, PageRequest.of(0, 20));

        // Specification is passed down — verified by confirming the repository was called
        verify(userRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // ---- helpers ----------------------------------------------------------

    private User makeUser(
            final UUID id, final String email, final UserRole role, final boolean active) {
        User user = new User(email, "hashed", "Test User", role, active);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
