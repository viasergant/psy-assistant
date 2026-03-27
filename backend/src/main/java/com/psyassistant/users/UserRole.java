package com.psyassistant.users;

/**
 * Roles available to internal CRM users.
 *
 * <p>Role names are prefixed with {@code ROLE_} when stored as JWT claim values
 * so that Spring Security recognises them without additional conversion.
 */
public enum UserRole {

    /** Full administrative access. */
    ADMIN,

    /** Standard therapist / staff access. */
    USER
}
