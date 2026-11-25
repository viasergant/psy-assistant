package com.psyassistant.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TenantContext#require()}. */
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requireReturnsBoundOrganization() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);
        assertThat(TenantContext.require()).isEqualTo(orgId);
    }

    @Test
    void requireFailsFastWhenNoOrganizationBound() {
        assertThatThrownBy(TenantContext::require).isInstanceOf(IllegalStateException.class);
    }
}
