package com.psyassistant.common.mode;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated endpoint that exposes the current deployment mode and application version
 * so Angular can discover the mode at runtime.
 *
 * <p>No {@code @Profile} annotation — present in both solo and clinic modes.
 * No {@code @PreAuthorize} — publicly accessible; permitted by {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/meta")
public class AppMetaController {

    private static final String MODE_SOLO = "SOLO";
    private static final String MODE_CLINIC = "CLINIC";

    private final Environment environment;

    /**
     * Constructs the controller with the Spring {@link Environment}.
     *
     * @param environment Spring environment used to read active profiles and version property
     */
    public AppMetaController(final Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns the current deployment mode and application version.
     *
     * <p>Mode resolution priority: {@code solo} wins over {@code clinic} if both profiles are
     * somehow active simultaneously (consistent with {@link DeploymentModeValidator} behaviour).
     *
     * @return {@link AppMetaResponse} with {@code mode} and {@code version}
     */
    @GetMapping
    public AppMetaResponse getMeta() {
        String[] profiles = environment.getActiveProfiles();

        String mode = MODE_SOLO;
        boolean foundClinic = false;
        for (String p : profiles) {
            if ("clinic".equals(p)) {
                foundClinic = true;
                break;
            }
        }
        if (foundClinic) {
            mode = MODE_CLINIC;
        }
        for (String p : profiles) {
            if ("solo".equals(p)) {
                mode = MODE_SOLO;
                break;
            }
        }

        String version = environment.getProperty("spring.application.version",
                environment.getProperty("build.version", "unknown"));

        return new AppMetaResponse(mode, version);
    }
}
