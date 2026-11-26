package com.psyassistant.common.tenant;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the tenant isolation infrastructure into the Spring MVC and JPA layers.
 *
 * <ul>
 *   <li>Registers {@link TenantInterceptor} to populate {@link TenantContext} from
 *       each incoming JWT before the controller executes.</li>
 *   <li>Installs {@link TenantHibernateJpaDialect} on the entity manager factory, which the
 *       {@code JpaTransactionManager} takes its dialect from, activating the Hibernate
 *       {@code orgFilter} at transaction-begin time. (Spring Boot does NOT pick up a
 *       standalone {@code JpaDialect} bean, so it must be set on the factory.)</li>
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

    /**
     * Sets the tenant-aware dialect on the entity manager factory before it initialises.
     *
     * @return post-processor installing {@link TenantHibernateJpaDialect}
     */
    @Bean
    public static BeanPostProcessor tenantJpaDialectInstaller() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
                if (bean instanceof AbstractEntityManagerFactoryBean emf) {
                    emf.setJpaDialect(new TenantHibernateJpaDialect());
                }
                return bean;
            }
        };
    }
}
