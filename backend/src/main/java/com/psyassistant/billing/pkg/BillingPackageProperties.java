package com.psyassistant.billing.pkg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for prepaid package behaviour.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * billing:
 *   packages:
 *     rollover: false
 *     grace-period-days: 0
 *     expiry-cron: "0 0 1 * * *"
 * </pre>
 */
@ConfigurationProperties(prefix = "billing.packages")
@Validated
public record BillingPackageProperties(
    boolean rollover,
    int gracePeriodDays,
    String expiryCron
) { }
