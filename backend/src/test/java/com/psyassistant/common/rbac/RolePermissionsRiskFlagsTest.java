package com.psyassistant.common.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.common.security.Permission;
import com.psyassistant.common.security.RolePermissions;
import com.psyassistant.users.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the risk-flag permission assignments added in PA-27 Increment 2.
 *
 * <p>Tests are pure Java — no Spring context required.
 */
class RolePermissionsRiskFlagsTest {

    // ---- RECEPTION_ADMIN_STAFF -------------------------------------------

    @Test
    void receptionAdminStaffContainsReadRiskFlags() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.RECEPTION_ADMIN_STAFF);
        assertThat(permissions).contains(Permission.READ_RISK_FLAGS);
    }

    @Test
    void receptionAdminStaffDoesNotContainManageRiskFlags() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.RECEPTION_ADMIN_STAFF);
        assertThat(permissions).doesNotContain(Permission.MANAGE_RISK_FLAGS);
    }

    @Test
    void receptionAdminStaffDoesNotContainReadRiskFlagNotes() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.RECEPTION_ADMIN_STAFF);
        assertThat(permissions).doesNotContain(Permission.READ_RISK_FLAG_NOTES);
    }

    @Test
    void receptionAdminStaffDoesNotContainManageRiskFlagTypes() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.RECEPTION_ADMIN_STAFF);
        assertThat(permissions).doesNotContain(Permission.MANAGE_RISK_FLAG_TYPES);
    }

    // ---- THERAPIST -------------------------------------------------------

    @Test
    void therapistContainsManageRiskFlags() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.THERAPIST);
        assertThat(permissions).contains(Permission.MANAGE_RISK_FLAGS);
    }

    @Test
    void therapistContainsReadRiskFlags() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.THERAPIST);
        assertThat(permissions).contains(Permission.READ_RISK_FLAGS);
    }

    @Test
    void therapistContainsReadRiskFlagNotes() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.THERAPIST);
        assertThat(permissions).contains(Permission.READ_RISK_FLAG_NOTES);
    }

    @Test
    void therapistDoesNotContainManageRiskFlagTypes() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.THERAPIST);
        assertThat(permissions).doesNotContain(Permission.MANAGE_RISK_FLAG_TYPES);
    }

    // ---- SUPERVISOR ------------------------------------------------------

    @Test
    void supervisorContainsManageRiskFlags() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.SUPERVISOR);
        assertThat(permissions).contains(Permission.MANAGE_RISK_FLAGS);
    }

    @Test
    void supervisorContainsReadRiskFlags() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.SUPERVISOR);
        assertThat(permissions).contains(Permission.READ_RISK_FLAGS);
    }

    @Test
    void supervisorContainsReadRiskFlagNotes() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.SUPERVISOR);
        assertThat(permissions).contains(Permission.READ_RISK_FLAG_NOTES);
    }

    @Test
    void supervisorDoesNotContainManageRiskFlagTypes() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.SUPERVISOR);
        assertThat(permissions).doesNotContain(Permission.MANAGE_RISK_FLAG_TYPES);
    }

    // ---- FINANCE ---------------------------------------------------------

    @Test
    void financeDoesNotContainAnyRiskFlagPermissions() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.FINANCE);
        assertThat(permissions).doesNotContainAnyElementsOf(Set.of(
                Permission.MANAGE_RISK_FLAGS,
                Permission.READ_RISK_FLAGS,
                Permission.READ_RISK_FLAG_NOTES,
                Permission.MANAGE_RISK_FLAG_TYPES
        ));
    }

    // ---- SYSTEM_ADMINISTRATOR --------------------------------------------

    @Test
    void systemAdministratorContainsAllFourRiskFlagPermissions() {
        Set<Permission> permissions = RolePermissions.permissionsFor(UserRole.SYSTEM_ADMINISTRATOR);
        assertThat(permissions).containsAll(Set.of(
                Permission.MANAGE_RISK_FLAGS,
                Permission.READ_RISK_FLAGS,
                Permission.READ_RISK_FLAG_NOTES,
                Permission.MANAGE_RISK_FLAG_TYPES
        ));
    }

    // ---- Completeness of Permission enum ---------------------------------

    @Test
    void allFourRiskFlagConstantsDeclaredInPermissionEnum() {
        Set<Permission> allPermissions = Set.of(Permission.values());
        assertThat(allPermissions).containsAll(Set.of(
                Permission.MANAGE_RISK_FLAGS,
                Permission.READ_RISK_FLAGS,
                Permission.READ_RISK_FLAG_NOTES,
                Permission.MANAGE_RISK_FLAG_TYPES
        ));
    }
}
