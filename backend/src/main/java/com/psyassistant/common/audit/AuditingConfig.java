package com.psyassistant.common.audit;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables JPA auditing and supplies the current auditor (principal name) for {@code @CreatedBy}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    /**
     * Provides the name of the currently authenticated principal.
     *
     * <p>Returns {@code Optional.empty()} when no authentication is present
     * (e.g. during unauthenticated requests or background jobs), which causes
     * Spring Data to leave the {@code createdBy} field unset.
     *
     * @return {@link AuditorAware} implementation backed by Spring Security context
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.empty();
            }
            return Optional.of(auth.getName());
        };
    }
}
