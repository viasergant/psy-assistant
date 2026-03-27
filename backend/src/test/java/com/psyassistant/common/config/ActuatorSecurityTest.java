package com.psyassistant.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Security tests for Actuator endpoints under the {@code prod} profile.
 *
 * <p>Verifies that sensitive Actuator endpoints ({@code /actuator/env},
 * {@code /actuator/beans}) are blocked unconditionally, and that
 * {@code /actuator/health} remains accessible without authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "prod"})
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * GET /actuator/env without credentials must return 401 or 403.
     */
    @Test
    void actuatorEnvWithoutAuthIsBlocked() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * GET /actuator/beans without credentials must return 401 or 403.
     */
    @Test
    void actuatorBeansWithoutAuthIsBlocked() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * GET /actuator/health without credentials must return 200.
     */
    @Test
    void actuatorHealthWithoutAuthIsPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
