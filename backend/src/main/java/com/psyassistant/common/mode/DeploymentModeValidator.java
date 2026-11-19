package com.psyassistant.common.mode;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Fails fast at context initialisation if neither the {@code solo} nor the {@code clinic}
 * Spring profile is active.
 *
 * <p>Registered as an {@link ApplicationContextInitializer} via
 * {@code META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports}
 * (Spring Boot 3.x registration). This runs before any bean is created, so a misconfigured
 * deployment is detected immediately on startup.
 *
 * <p>This class intentionally carries no Spring bean annotations ({@code @Component},
 * {@code @Service}, etc.) — it is a pure {@link ApplicationContextInitializer}.
 */
public class DeploymentModeValidator
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        String[] profiles = applicationContext.getEnvironment().getActiveProfiles();
        boolean hasSolo = false;
        boolean hasClinic = false;
        boolean hasSaas = false;
        for (String p : profiles) {
            if ("solo".equals(p)) {
                hasSolo = true;
            }
            if ("clinic".equals(p)) {
                hasClinic = true;
            }
            if ("saas".equals(p)) {
                hasSaas = true;
            }
        }
        if (!hasSolo && !hasClinic && !hasSaas) {
            throw new IllegalStateException(
                "Deployment mode not configured. "
                + "Start with --spring.profiles.active=solo, clinic, or saas"
            );
        }
    }
}
