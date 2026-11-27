package com.psyassistant.admin;

/** Why a user is {@link UserPresence#BLOCKED}. */
public enum BlockReason {
    /** Account deactivated by an administrator. */
    SUSPENDED,
    /** Temporarily locked after repeated failed logins. */
    LOCKED
}
