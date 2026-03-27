package com.psyassistant.common.security;

import com.psyassistant.users.UserRole;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single authoritative source of the role-to-permission matrix.
 *
 * <p>Adding or revoking a permission for a role requires a change in exactly
 * one place — the {@link #PERMISSIONS} map below.  No annotation changes are
 * needed because service-layer {@code @PreAuthorize} annotations reference
 * {@link Permission} values, which are included in the JWT by
 * {@link com.psyassistant.auth.service.TokenService}.
 *
 * <p>Permission matrix:
 * <pre>
 * Permission               | RAS | THE | SUP | FIN | SYS
 * -------------------------|-----|-----|-----|-----|-----
 * MANAGE_CLIENTS           |  Y  |     |     |     |  Y
 * MANAGE_APPOINTMENTS      |  Y  |     |     |     |  Y
 * MANAGE_LEADS             |  Y  |     |     |     |  Y
 * READ_OWN_SESSIONS        |     |  Y  |     |     |
 * READ_ALL_SESSIONS        |     |     |  Y  |     |  Y
 * WRITE_SESSION_NOTE       |     |  Y  |     |     |
 * READ_OWN_SESSION_NOTES   |     |  Y  |     |     |
 * READ_ALL_SESSION_NOTES   |     |     |  Y  |     |  Y
 * READ_CARE_PLANS          |     |  Y  |  Y  |     |  Y
 * MANAGE_INVOICES          |     |     |     |  Y  |  Y
 * MANAGE_PAYMENTS          |     |     |     |  Y  |  Y
 * READ_FINANCIAL_REPORTS   |     |     |     |  Y  |  Y
 * READ_TEAM_WORKLOAD       |     |     |  Y  |     |  Y
 * READ_REPORTS             |     |     |  Y  |     |  Y
 * MANAGE_USERS             |     |     |     |     |  Y
 * VIEW_AUDIT_LOG           |     |     |     |     |  Y
 * MANAGE_SYSTEM_CONFIG     |     |     |     |     |  Y
 *
 * RAS = RECEPTION_ADMIN_STAFF, THE = THERAPIST, SUP = SUPERVISOR,
 * FIN = FINANCE,               SYS = SYSTEM_ADMINISTRATOR
 * </pre>
 */
public final class RolePermissions {

    /**
     * Immutable map from {@link UserRole} to the set of {@link Permission}s it grants.
     * This is the single authoritative source for the permission matrix.
     */
    public static final Map<UserRole, Set<Permission>> PERMISSIONS;

    static {
        EnumMap<UserRole, Set<Permission>> map = new EnumMap<>(UserRole.class);

        map.put(UserRole.RECEPTION_ADMIN_STAFF, Collections.unmodifiableSet(EnumSet.of(
                Permission.MANAGE_CLIENTS,
                Permission.MANAGE_APPOINTMENTS,
                Permission.MANAGE_LEADS
        )));

        map.put(UserRole.THERAPIST, Collections.unmodifiableSet(EnumSet.of(
                Permission.READ_OWN_SESSIONS,
                Permission.WRITE_SESSION_NOTE,
                Permission.READ_OWN_SESSION_NOTES,
                Permission.READ_CARE_PLANS
        )));

        map.put(UserRole.SUPERVISOR, Collections.unmodifiableSet(EnumSet.of(
                Permission.READ_ALL_SESSIONS,
                Permission.READ_ALL_SESSION_NOTES,
                Permission.READ_CARE_PLANS,
                Permission.READ_TEAM_WORKLOAD,
                Permission.READ_REPORTS
        )));

        map.put(UserRole.FINANCE, Collections.unmodifiableSet(EnumSet.of(
                Permission.MANAGE_INVOICES,
                Permission.MANAGE_PAYMENTS,
                Permission.READ_FINANCIAL_REPORTS
        )));

        map.put(UserRole.SYSTEM_ADMINISTRATOR, Collections.unmodifiableSet(EnumSet.of(
                Permission.MANAGE_CLIENTS,
                Permission.MANAGE_APPOINTMENTS,
                Permission.MANAGE_LEADS,
                Permission.READ_ALL_SESSIONS,
                Permission.READ_ALL_SESSION_NOTES,
                Permission.READ_CARE_PLANS,
                Permission.MANAGE_INVOICES,
                Permission.MANAGE_PAYMENTS,
                Permission.READ_FINANCIAL_REPORTS,
                Permission.READ_TEAM_WORKLOAD,
                Permission.READ_REPORTS,
                Permission.MANAGE_USERS,
                Permission.VIEW_AUDIT_LOG,
                Permission.MANAGE_SYSTEM_CONFIG
        )));

        // Deprecated legacy roles — no permissions; treated as THERAPIST/SYSTEM_ADMINISTRATOR
        // after migration. Kept here to prevent NPE on un-migrated tokens.
        map.put(UserRole.ADMIN, Collections.emptySet());
        map.put(UserRole.USER, Collections.emptySet());

        PERMISSIONS = Collections.unmodifiableMap(map);
    }

    private RolePermissions() {
        // utility class
    }

    /**
     * Returns the set of permissions for the given role.
     *
     * @param role the role to look up
     * @return immutable set of permissions; never null, may be empty
     */
    public static Set<Permission> permissionsFor(final UserRole role) {
        return PERMISSIONS.getOrDefault(role, Collections.emptySet());
    }
}
