package com.psyassistant.auth.token;

/**
 * Reason a {@link OneTimeToken} was issued. A token is only valid for its own purpose.
 */
public enum TokenPurpose {

    /** Confirms ownership of the email address used for self-service registration. */
    EMAIL_CONFIRMATION
}
