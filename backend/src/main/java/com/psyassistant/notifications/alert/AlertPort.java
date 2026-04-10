package com.psyassistant.notifications.alert;

import java.util.UUID;

/**
 * Port for raising operational alerts when email delivery permanently fails.
 *
 * <p>The production implementation is responsible for notifying on-call engineers
 * through whatever alerting channel is configured (PagerDuty, Slack, etc.).
 * The current default implementation is {@link LoggingAlertAdapter}, which logs at ERROR.
 */
public interface AlertPort {

    /**
     * Raises an alert indicating that email delivery for the given log entry has permanently
     * failed after exhausting all retry attempts.
     *
     * @param logId the {@link com.psyassistant.notifications.log.EmailDeliveryLog} entity ID;
     *              must not be {@code null}
     */
    void raiseDeliveryFailedAlert(UUID logId);
}
