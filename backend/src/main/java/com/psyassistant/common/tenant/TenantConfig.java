package com.psyassistant.common.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the tenant isolation infrastructure into the Spring MVC and JPA layers.
 *
 * <ul>
 *   <li>Registers {@link TenantInterceptor} to populate {@link TenantContext} from
 *       each incoming JWT before the controller executes.</li>
 *   <li>Exposes {@link TenantHibernateJpaDialect} as a {@link JpaDialect} bean so
 *       Spring Boot's {@code JpaBaseConfiguration} auto-wires it into the
 *       {@code JpaTransactionManager}, activating the Hibernate {@code orgFilter}
 *       at transaction-begin time.</li>
 * </ul>
 */
@Configuration
public class TenantConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    public TenantConfig(final TenantInterceptor tenantInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor);
    }

    @Bean
    public JpaDialect jpaDialect() {
        return new TenantHibernateJpaDialect();
    }
}
