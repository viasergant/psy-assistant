package com.psyassistant.billing.pkg;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that marks expired prepaid package instances as EXPIRED.
 *
 * <p>Runs once daily at the time configured by {@code billing.packages.expiry-cron}
 * (default: {@code 0 0 1 * * *} — 01:00 server time).
 */
@Component
public class PackageExpiryScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PackageExpiryScheduler.class);

    private final PackageService packageService;
    private final BillingPackageProperties properties;

    public PackageExpiryScheduler(
            final PackageService packageService,
            final BillingPackageProperties properties) {
        this.packageService = packageService;
        this.properties = properties;
    }

    /**
     * Expires all package instances whose {@code expires_at} date is before today.
     *
     * <p>The cron expression is read from {@link BillingPackageProperties#expiryCron()}.
     * Disable in tests by setting {@code billing.packages.expiry-cron="-"} or by
     * setting Spring's scheduling to a disabled state in the test profile.
     */
    @Scheduled(cron = "${billing.packages.expiry-cron}")
    public void expireOverduePackages() {
        LocalDate today = LocalDate.now();
        LOG.debug("Package expiry job starting for date {}", today);
        packageService.expirePackages(today);
    }
}
