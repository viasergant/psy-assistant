package com.psyassistant.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Reads the {@code orgId} JWT claim and stores it in {@link TenantContext} for the
 * duration of the request. Clears the context unconditionally in
 * {@link #afterCompletion} to prevent ThreadLocal leaks across thread-pool reuse.
 *
 * <p>Requests without a JWT (public endpoints such as {@code /api/v1/register}) are
 * handled gracefully: no {@code orgId} is set and the Hibernate filter remains
 * inactive for that transaction, which is the correct behaviour for endpoints that
 * write rows into a new organization without yet having an org context.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String orgIdStr = jwtAuth.getToken().getClaimAsString("orgId");
            if (orgIdStr != null) {
                try {
                    TenantContext.set(java.util.UUID.fromString(orgIdStr));
                } catch (IllegalArgumentException ignored) {
                    // malformed claim — treat as missing; requests will see no data
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final Object handler,
                                final Exception ex) {
        TenantContext.clear();
    }
}
