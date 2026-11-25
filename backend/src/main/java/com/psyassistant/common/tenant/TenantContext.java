package com.psyassistant.common.tenant;

import java.util.UUID;

/**
 * Carries the current organization ID for the duration of a single HTTP request.
 *
 * <p>Populated by {@link TenantInterceptor} from the JWT {@code orgId} claim, and
 * consumed by {@link TenantHibernateJpaDialect} to activate the Hibernate
 * {@code orgFilter} at transaction-begin time.
 *
 * <p>Must be cleared after each request to prevent ThreadLocal leaks across
 * pooled threads (handled by {@link TenantInterceptor#afterCompletion}).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_ORG = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(final UUID orgId) {
        CURRENT_ORG.set(orgId);
    }

    public static UUID get() {
        return CURRENT_ORG.get();
    }

    /**
     * Returns the current organization id, failing fast when none is bound.
     *
     * <p>Raw SQL bypasses the Hibernate {@code orgFilter}; callers of such queries must
     * scope by this value explicitly.
     *
     * @return current organization id, never null
     * @throws IllegalStateException if no organization is bound to the current thread
     */
    public static UUID require() {
        UUID orgId = CURRENT_ORG.get();
        if (orgId == null) {
            throw new IllegalStateException("No organization bound to the current request");
        }
        return orgId;
    }

    public static void clear() {
        CURRENT_ORG.remove();
    }
}
