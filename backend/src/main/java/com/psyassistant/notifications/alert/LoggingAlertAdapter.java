package com.psyassistant.notifications.alert;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of {@link AlertPort} that logs at ERROR level.
 *
 * <p>Replace with a real alerting integration (e.g. PagerDuty, Slack) when available.
 * Marked {@link Primary} so it is selected when no other implementation is present.
 */
@Component
@Primary
public class LoggingAlertAdapter implements AlertPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingAlertAdapter.class);

    /**
     * {@inheritDoc}
     *
     * <p>Logs an ERROR-level message. This stub does not send external alerts.
     */
    @Override
    public void raiseDeliveryFailedAlert(final UUID logId) {
        LOG.error("EMAIL_DELIVERY_PERMANENTLY_FAILED logId={} — manual intervention required",
                logId);
    }
}
