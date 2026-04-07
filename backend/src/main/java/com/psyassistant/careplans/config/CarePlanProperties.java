package com.psyassistant.careplans.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configures care-plan domain rules loaded from {@code app.care-plans.*}.
 *
 * <p>Example {@code application.yml} configuration:
 * <pre>
 * app:
 *   care-plans:
 *     max-active-per-client: 3
 *     intervention-types:
 *       - CBT
 *       - DBT
 * </pre>
 */
@ConfigurationProperties(prefix = "app.care-plans")
@Validated
public record CarePlanProperties(
    @Min(1) @Max(20) int maxActivePerClient,
    @NotEmpty List<String> interventionTypes
) { }
