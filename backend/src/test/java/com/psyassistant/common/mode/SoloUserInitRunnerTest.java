package com.psyassistant.common.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.common.audit.AuditLog;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.organization.OrganizationStatus;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.therapists.service.SingleTherapistProfileProvisioner;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link SoloUserInitRunner}.
 *
 * <p>All dependencies are mocked via Mockito. {@code @Value}-injected fields are set via
 * {@link ReflectionTestUtils} to avoid a full Spring context.
 */
@ExtendWith(MockitoExtension.class)
class SoloUserInitRunnerTest {

    private static final String TEST_USERNAME = "solo@example.com";
    private static final String TEST_PASSWORD = "S3cr3tP@ss!";
    private static final String TEST_HASH = "$2a$12$hashedpassword";

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SingleTherapistProfileProvisioner therapistProvisioner;

    @Mock
    private ApplicationArguments applicationArguments;

    private SoloUserInitRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SoloUserInitRunner(userRepository, organizationRepository,
                passwordEncoder, auditLogService, therapistProvisioner);
        ReflectionTestUtils.setField(runner, "soloUsername", TEST_USERNAME);
        ReflectionTestUtils.setField(runner, "soloPassword", TEST_PASSWORD);
    }

    @Test
    void whenNoUsersExistCreatesUserWithAllRolesAndMustChangePassword() throws Exception {
        // Arrange
        when(organizationRepository.count()).thenReturn(0L);
        Organization savedOrg = new Organization(
                "Solo Practice", "solo", OrganizationType.SOLO, OrganizationStatus.ACTIVE);
        ReflectionTestUtils.setField(savedOrg, "id", UUID.randomUUID());
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_HASH);

        User savedUser = new User(TEST_USERNAME, TEST_HASH,
            java.util.EnumSet.of(
                UserRole.RECEPTION_ADMIN_STAFF, UserRole.THERAPIST,
                UserRole.SUPERVISOR, UserRole.FINANCE, UserRole.SYSTEM_ADMINISTRATOR
            ), true);
        savedUser.setMustChangePassword(true);
        savedUser.setOrgId(savedOrg.getId());
        ReflectionTestUtils.setField(savedUser, "id", UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        runner.run(applicationArguments);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertTrue(captured.isMustChangePassword(), "mustChangePassword must be true");
        assertTrue(captured.isActive(), "user must be active");

        java.util.Set<UserRole> roles = captured.getRoles();
        assertTrue(roles.contains(UserRole.RECEPTION_ADMIN_STAFF), "missing RECEPTION_ADMIN_STAFF");
        assertTrue(roles.contains(UserRole.THERAPIST), "missing THERAPIST");
        assertTrue(roles.contains(UserRole.SUPERVISOR), "missing SUPERVISOR");
        assertTrue(roles.contains(UserRole.FINANCE), "missing FINANCE");
        assertTrue(roles.contains(UserRole.SYSTEM_ADMINISTRATOR), "missing SYSTEM_ADMINISTRATOR");

        verify(therapistProvisioner).ensureProfile(savedUser);
        verify(auditLogService).record(any(AuditLog.class));
    }

    @Test
    void whenUsersAlreadyExistSkipsCreation() throws Exception {
        // Arrange — idempotency guard now checks organizationRepository
        when(organizationRepository.count()).thenReturn(1L);

        // Act
        runner.run(applicationArguments);

        // Assert
        verify(userRepository, never()).save(any(User.class));
        verify(auditLogService, never()).record(any(AuditLog.class));
    }

    @Test
    void whenSoloUsernameBlankThrowsIllegalStateException() {
        // Arrange
        ReflectionTestUtils.setField(runner, "soloUsername", "");
        when(organizationRepository.count()).thenReturn(0L);

        // Act + Assert
        assertThrows(
            IllegalStateException.class,
            () -> runner.run(applicationArguments),
            "Expected IllegalStateException when SOLO_USERNAME is blank"
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenSoloPasswordBlankThrowsIllegalStateException() throws Exception {
        // Arrange
        when(organizationRepository.count()).thenReturn(0L);
        ReflectionTestUtils.setField(runner, "soloUsername", "admin@example.com");
        ReflectionTestUtils.setField(runner, "soloPassword", "");

        // Act + Assert
        assertThrows(
            IllegalStateException.class,
            () -> runner.run(applicationArguments),
            "Expected IllegalStateException when SOLO_PASSWORD is blank"
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenAuditLogThrowsRunCompletesNormally() throws Exception {
        // Arrange
        when(organizationRepository.count()).thenReturn(0L);
        Organization org = new Organization(
                "Solo Practice", "solo", OrganizationType.SOLO, OrganizationStatus.ACTIVE);
        ReflectionTestUtils.setField(org, "id", UUID.randomUUID());
        when(organizationRepository.save(any(Organization.class))).thenReturn(org);
        ReflectionTestUtils.setField(runner, "soloUsername", "admin@example.com");
        ReflectionTestUtils.setField(runner, "soloPassword", "SecurePass1!");
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("audit DB down")).when(auditLogService).record(any());

        // Act + Assert — audit failure must not propagate
        assertDoesNotThrow(() -> runner.run(applicationArguments));
        verify(userRepository).save(any(User.class));
    }
}
