package com.psyassistant.notifications;

import java.util.UUID;

/**
 * Port for billing-related notifications.
 *
 * <p>Implementations send notifications to clients and finance staff.
 * The current production implementation is a stub that logs only.
 */
public interface BillingNotificationPort {

    /**
     * Notify relevant parties that a client's prepaid package has been exhausted.
     *
     * @param clientId          the client's ID (opaque)
     * @param packageInstanceId the exhausted package instance ID
     */
    void notifyPackageExhausted(UUID clientId, UUID packageInstanceId);

    /**
     * Notify relevant parties that a client's prepaid package has expired with unused sessions.
     *
     * @param clientId          the client's ID (opaque)
     * @param packageInstanceId the expired package instance ID
     * @param unusedSessions    number of sessions that were not used
     */
    void notifyPackageExpired(UUID clientId, UUID packageInstanceId, int unusedSessions);
}
