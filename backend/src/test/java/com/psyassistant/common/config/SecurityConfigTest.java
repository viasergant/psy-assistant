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
 * Verifies the Spring Security filter chain configuration:
 * <ul>
 *   <li>The actuator health endpoint is accessible without authentication.</li>
 *   <li>Any other endpoint without authentication returns 401 Unauthorized.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * GET /actuator/health without an Authorization header must return 200.
     */
    @Test
    void healthEndpointIsPermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    /**
     * Any non-public endpoint without an Authorization header must return 401.
     */
    @Test
    void nonPublicEndpointWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/some-protected-resource"))
                .andExpect(status().isUnauthorized());
    }
}
