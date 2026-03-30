package com.psyassistant.users;

/**
 * Roles available to internal CRM users.
 *
 * <p>Role names are prefixed with {@code ROLE_} when stored as JWT claim values
 * so that Spring Security recognises them without additional conversion.
 *
 * <p>The legacy {@link #ADMIN} and {@link #USER} values are kept as deprecated
 * aliases to support gradual migration; the Flyway {@code V5} script migrates
 * persisted rows to the scoped role names. New code must use the scoped values.
 */
public enum UserRole {

    /** Reception / admin staff: manage leads, clients, appointments, reminders, basic billing. */
    RECEPTION_ADMIN_STAFF,

    /** Therapist: view assigned clients, manage own sessions, write session notes, care plans. */
    THERAPIST,

    /** Supervisor: view team workload, full clinical notes, reports, selected summaries. */
    SUPERVISOR,

    /** Finance: manage invoices, payments, refunds, financial reporting. */
    FINANCE,

    /** System administrator: manage users, roles, system configuration, audit log access. */
    SYSTEM_ADMINISTRATOR,

    /**
     * Full administrative access.
     *
     * @deprecated Replaced by {@link #SYSTEM_ADMINISTRATOR}. Migrated via {@code V5__rbac_roles.sql}.
     */
    @Deprecated
    ADMIN,

    /**
     * Standard therapist / staff access.
     *
     * @deprecated Replaced by {@link #THERAPIST}. Migrated via {@code V5__rbac_roles.sql}.
     */
    @Deprecated
    USER;

    /**
     * Returns the persisted scoped role for this value.
     *
     * <p>Legacy aliases are mapped to their RBAC replacements so writes remain
     * compatible with the DB {@code chk_user_role} constraint.
     *
     * @return canonical persisted role
     */
    public UserRole canonical() {
        return switch (this) {
            case ADMIN -> SYSTEM_ADMINISTRATOR;
            case USER -> THERAPIST;
            default -> this;
        };
    }

    /**
     * @return true if this is a deprecated alias role
     */
    public boolean isLegacyAlias() {
        return this == ADMIN || this == USER;
    }
}
