package com.psyassistant.admin;

/** Presence of a user as shown to the global administrator. */
public enum UserPresence {
    /** Active recently and not blocked. */
    ONLINE,
    /** Not blocked, but not active recently. */
    OFFLINE,
    /** Suspended by an administrator or temporarily locked. */
    BLOCKED
}
