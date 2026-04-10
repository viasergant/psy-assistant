package com.psyassistant.reporting.reports;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the reports feature (PA-55).
 *
 * <p>Example {@code application.yml} configuration:
 * <pre>
 * app:
 *   reports:
 *     churn-threshold-days: 90
 *     max-history-years: 3
 *     export-row-cap: 50000
 *     default-page-size: 25
 * </pre>
 */
@ConfigurationProperties(prefix = "app.reports")
@Validated
public record ReportProperties(
    @Min(1) @Max(365) int churnThresholdDays,
    @Positive int maxHistoryYears,
    @Positive int exportRowCap,
    @Min(1) @Max(100) int defaultPageSize
) {
}
