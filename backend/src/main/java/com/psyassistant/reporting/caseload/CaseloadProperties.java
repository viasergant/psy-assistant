package com.psyassistant.reporting.caseload;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the caseload overview feature.
 *
 * <p>Example {@code application.yml} configuration:
 * <pre>
 * app:
 *   caseload:
 *     active-client-days: 90
 *     job-enabled: true
 *     job-cron: "0 0 2 * * *"
 * </pre>
 */
@ConfigurationProperties(prefix = "app.caseload")
@Validated
public record CaseloadProperties(
    @Min(1) @Max(365) int activeClientDays,
    boolean jobEnabled,
    String jobCron
) {
}
