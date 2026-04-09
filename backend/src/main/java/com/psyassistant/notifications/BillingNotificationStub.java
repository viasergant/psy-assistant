package com.psyassistant.notifications;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of {@link BillingNotificationPort}.
 *
 * <p>Logs the notification intent only — email/push delivery is not yet implemented.
 * Replace this class with a real implementation when the notification channel is available.
 */
@Component
@Primary
public class BillingNotificationStub implements BillingNotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(BillingNotificationStub.class);

    @Override
    public void notifyPackageExhausted(final UUID clientId, final UUID packageInstanceId) {
        LOG.info("STUB: Package exhausted - clientId={}, packageId={}", clientId, packageInstanceId);
    }

    @Override
    public void notifyPackageExpired(final UUID clientId, final UUID packageInstanceId, final int unusedSessions) {
        LOG.info("STUB: Package expired - clientId={}, packageId={}, unusedSessions={}",
                clientId, packageInstanceId, unusedSessions);
    }
}
