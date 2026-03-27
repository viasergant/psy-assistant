package com.psyassistant.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Observability extension point for APM and custom Micrometer metrics.
 *
 * <h2>Attaching an APM agent</h2>
 * <p>No source-code change is needed. Add the Java agent JVM argument at startup:
 * <pre>{@code
 * java -javaagent:/opt/apm/dd-java-agent.jar \
 *      -Ddd.service=psy-assistant \
 *      -Ddd.env=prod \
 *      -jar app.jar
 * }</pre>
 *
 * <h2>Adding a Micrometer registry</h2>
 * <p>Add the registry dependency to {@code pom.xml} (e.g.,
 * {@code micrometer-registry-datadog}) and, if needed, configure it here via a
 * {@code MeterRegistryCustomizer} bean. Example (commented out below).
 *
 * <h2>PII masking</h2>
 * <p>Never log raw personal data. Log only opaque identifiers (e.g., {@code clientId=42},
 * {@code sessionId=abc-123}). See {@code docs/adr/001-observability.md} for the full policy.
 *
 * <p>See {@code docs/adr/001-observability.md} for complete rationale and decision record.
 */
@Configuration
public class ObservabilityConfig {

    // Example: uncomment and configure to add a custom Micrometer registry.
    // @Bean
    // public MeterRegistryCustomizer<MeterRegistry> commonTags(
    //         @Value("${spring.application.name}") String appName) {
    //     return registry -> registry.config().commonTags("application", appName);
    // }
}
